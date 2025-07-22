package com.playdata.attendanceservice.attendance.scheduler;

import com.playdata.attendanceservice.attendance.entity.WorkDayType;
import com.playdata.attendanceservice.attendance.entity.WorkStatus;
import com.playdata.attendanceservice.attendance.repository.WorkStatusRepository;
import com.playdata.attendanceservice.client.HrServiceClient;
import com.playdata.attendanceservice.client.VacationServiceClient;
import com.playdata.attendanceservice.client.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.playdata.attendanceservice.common.dto.CommonResDto;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaveAccrualScheduler {

    private final WorkStatusRepository workStatusRepository;
    private final HrServiceClient hrServiceClient;
    private final VacationServiceClient vacationServiceClient;

    @Scheduled(cron = "0 0 1 1 * ?") // 매월 1일 새벽 1시
    public void grantFirstYearAnnualLeave() {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();

        log.info("{}년 {}월 기준, 입사 1주년 연차 부여 스케줄을 시작합니다.", currentYear, currentMonth);

        try {
            // 1. hr-service에서 이번 달에 입사 1주년이 되는 사용자 목록 조회
            // (hr-service의 getUsersWithFirstAnniversaryInMonth는 입사일이 'year-1'년 'month'월인 사용자를 반환)
            List<UserDto> users = hrServiceClient.getUsersWithFirstAnniversaryInMonth(currentYear - 1, currentMonth);
            log.info("총 {}명의 입사 1주년 대상자가 조회되었습니다.", users.size());

            if (users.isEmpty()) {
                log.info("이번 달에 입사 1주년 대상자가 없습니다. 스케줄을 종료합니다.");
                return;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

            for (UserDto user : users) {
                Long userId = user.getUserId();
                LocalDate hireDate = user.getHireDate(); // UserDto에 hireDate가 있다고 가정

                // 입사일로부터 1년이 되는 날짜 (연차 발생 기준일)
                LocalDate anniversaryDate = hireDate.plusYears(1);

                // 연차 발생 기준일이 현재 월에 속하는지 다시 한번 확인 (정확성을 위해)
                if (anniversaryDate.getYear() == currentYear && anniversaryDate.getMonthValue() == currentMonth) {

                    // 1년간의 출근율 계산 기간 설정 (입사일 ~ 1주년 전날)
                    LocalDate periodStart = hireDate;
                    LocalDate periodEnd = anniversaryDate.minusDays(1);

                    log.info("사용자 ID: {}, 입사일: {}, 1주년 기준일: {}. 출근율 계산 기간: {} ~ {}",
                            userId, hireDate, anniversaryDate, periodStart, periodEnd);

                    // 1년간의 실제 근무일수 계산
                    List<WorkStatus> workStatuses = workStatusRepository.findByUserIdInAndDateBetween(
                            List.of(userId),
                            periodStart,
                            periodEnd
                    );

                    double actualWorkDays = workStatuses.stream()
                            .filter(ws -> ws.getWorkDayType() != null)
                            .mapToDouble(ws -> {
                                if (ws.getWorkDayType() == WorkDayType.FULL_DAY) return 1.0;
                                else if (ws.getWorkDayType() == WorkDayType.HALF_DAY) return 0.5;
                                else return 0.0;
                            })
                            .sum();

                    // 1년간의 유급 휴가 일수 조회
                    Map<Long, Double> paidVacationDaysMap = vacationServiceClient.getApprovedPaidVacationDays(
                            List.of(userId),
                            periodStart.format(formatter),
                            periodEnd.format(formatter)
                    ).getResult();
                    double paidVacationDays = paidVacationDaysMap.getOrDefault(userId, 0.0);

                    // 총 인정 근무일수
                    double totalAccreditedWorkDays = actualWorkDays + paidVacationDays;

                    // 1년간의 총 소정근로일수 계산 (주말 제외, 공휴일 미고려)
                    long totalScheduledWorkDays = periodStart.datesUntil(periodEnd.plusDays(1))
                            .filter(date -> date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY)
                            .count();

                    // 출근율 계산
                    double attendanceRate = (totalScheduledWorkDays > 0) ? (totalAccreditedWorkDays / totalScheduledWorkDays) * 100 : 0.0;

                    log.info("사용자 ID: {}. 실제 근무일: {}, 유급 휴가일: {}, 총 인정 근무일: {}, 총 소정근로일: {}, 출근율: {}%",
                            userId, actualWorkDays, paidVacationDays, totalAccreditedWorkDays, totalScheduledWorkDays, String.format("%.2f", attendanceRate));

                    // 출근율 80% 이상인 경우 15일 연차 부여
                    if (attendanceRate >= 80.0) {
                        log.info("사용자 ID: {}. 출근율 {}%로 1주년 연차(15일) 부여 대상입니다.", userId, String.format("%.2f", attendanceRate));
                        vacationServiceClient.grantAnnualLeave(userId, 15);
                    } else {
                        log.info("사용자 ID: {}. 출근율 {}%로 1주년 연차 부여 대상이 아닙니다. (80% 미만)", userId, String.format("%.2f", attendanceRate));
                    }
                } else {
                    log.warn("사용자 ID: {}의 입사 1주년 기준일({})이 현재 스케줄 실행 월({})과 일치하지 않습니다. 건너뜁니다.",
                            userId, anniversaryDate, today.format(DateTimeFormatter.ofPattern("yyyy-MM")));
                }
            }

            log.info("입사 1주년 연차 부여 스케줄을 성공적으로 완료했습니다.");

        } catch (Exception e) {
            log.error("입사 1주년 연차 부여 스케줄 실행 중 오류가 발생했습니다.", e);
        }
    }

    @Scheduled(cron = "0 0 2 1 * ?")
    public void accrueMonthlyLeave() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        LocalDate startDate = lastMonth.atDay(1);
        LocalDate endDate = lastMonth.atEndOfMonth();
        log.info("{}년 {}월 근무 기록을 기준으로 연차 부여 스케줄을 시작합니다.", lastMonth.getYear(), lastMonth.getMonthValue());

        try {
            // 1. 모든 활성 사용자 ID 조회
            List<Long> allUserIds = hrServiceClient.getAllUserIds().stream()
                    .map(UserDto::getUserId)
                    .collect(Collectors.toList());

            if (allUserIds.isEmpty()) {
                log.info("조회된 사용자 ID가 없습니다. 연차 부여 스케줄을 종료합니다.");
                return;
            }

            // 2. 지난달의 모든 WorkStatus 기록 조회하여 실제 근무일수 계산
            List<WorkStatus> lastMonthWorkStatuses = workStatusRepository.findByUserIdInAndDateBetween(
                    allUserIds,
                    startDate,
                    endDate
            );

            Map<Long, Double> actualWorkDays = lastMonthWorkStatuses.stream()
                    .filter(ws -> ws.getWorkDayType() != null)
                    .collect(Collectors.groupingBy(
                            WorkStatus::getUserId,
                            Collectors.summingDouble(ws -> {
                                if (ws.getWorkDayType() == WorkDayType.FULL_DAY) {
                                    return 1.0;
                                } else if (ws.getWorkDayType() == WorkDayType.HALF_DAY) {
                                    return 0.5;
                                } else {
                                    return 0.0;
                                }
                            })
                    ));
            log.info("실제 근무일수 계산 결과: {}", actualWorkDays);

            // 3. vacation-service에서 승인된 유급 휴가 일수 조회
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
            CommonResDto<Map<Long, Double>> paidVacationDaysResponse = vacationServiceClient.getApprovedPaidVacationDays(
                    allUserIds,
                    startDate.format(formatter),
                    endDate.format(formatter)
            );
            Map<Long, Double> paidVacationDays = paidVacationDaysResponse.getResult();
            log.info("유급 휴가일수 조회 결과: {}", paidVacationDays);

            // 4. 최종 인정 근무일수 계산 (실제 근무일수 + 유급 휴가일수)
            Map<Long, Double> totalAccreditedWorkDays = allUserIds.stream()
                    .collect(Collectors.toMap(
                            userId -> userId,
                            userId -> actualWorkDays.getOrDefault(userId, 0.0) + paidVacationDays.getOrDefault(userId, 0.0)
                    ));
            log.info("최종 인정 근무일수 계산 결과: {}", totalAccreditedWorkDays);


            // 5. 연차 부여 조건(15일 이상)을 충족하는 사용자를 대상으로 연차 부여
            totalAccreditedWorkDays.forEach((userId, workDays) -> {
                if (workDays >= 15.0) { // 월 15일 이상 근무 시
                    log.info("사용자 ID: {}, 최종 인정 근무일수: {}. 월차 부여 대상입니다.", userId, workDays);
                    vacationServiceClient.grantMonthlyLeave(userId);
                } else {
                    log.info("사용자 ID: {}, 최종 인정 근무일수: {}. 월차 부여 대상이 아닙니다. (15일 미만)", userId, workDays);
                }
            });

            log.info("월별 연차 부여 스케줄을 성공적으로 완료했습니다.");

        } catch (Exception e) {
            log.error("월별 연차 부여 스케줄 실행 중 오류가 발생했습니다.", e);
        }
    }
}
