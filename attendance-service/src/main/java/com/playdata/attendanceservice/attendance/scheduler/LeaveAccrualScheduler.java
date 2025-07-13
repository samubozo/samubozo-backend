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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaveAccrualScheduler {

    private final WorkStatusRepository workStatusRepository;
    private final HrServiceClient hrServiceClient;
    private final VacationServiceClient vacationServiceClient;

    @Scheduled(cron = "0 0 1 * * ?")
    public void grantFirstYearAnnualLeave() {
        LocalDate today = LocalDate.now();
        log.info("{} 기준, 입사 1주년 연차 부여 스케줄을 시작합니다.", today);

        try {
            List<UserDto> users = hrServiceClient.getUsersWithFirstAnniversary(today);
            log.info("총 {}명의 입사 1주년 대상자가 조회되었습니다.", users.size());

            users.forEach(user -> {
                log.info("사용자 ID: {}. 1주년 연차(15일) 부여 대상입니다.", user.getUserId());
                vacationServiceClient.grantAnnualLeave(Long.valueOf(user.getUserId()), 15);
            });

            log.info("입사 1주년 연차 부여 스케줄을 성공적으로 완료했습니다.", today);

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
            // 1. 모든 사용자 ID 조회
            List<Long> allUserIds = hrServiceClient.getAllUserIds().stream()
                    .map(UserDto::getUserId)
                    .collect(Collectors.toList());

            if (allUserIds.isEmpty()) {
                log.info("조회된 사용자 ID가 없습니다. 연차 부여 스케줄을 종료합니다.");
                return;
            }

            // 2. 지난달의 모든 WorkStatus 기록 조회
            List<WorkStatus> lastMonthWorkStatuses = workStatusRepository.findByUserIdInAndDateBetween(
                    allUserIds,
                    startDate,
                    endDate
            );

            log.info("총 {}개의 지난달 근무 기록이 조회되었습니다.", lastMonthWorkStatuses.size());

            // 3. 사용자별 근무일수 계산 (전일: 1.0, 반일: 0.5)
            Map<Long, Double> userWorkDays = lastMonthWorkStatuses.stream()
                    .filter(ws -> ws.getWorkDayType() != null) // workDayType이 null이 아닌 경우만 처리
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

            log.info("근무일수 계산 결과: {}", userWorkDays);

            // 4. 연차 부여 조건(예: 15일 이상 근무)을 충족하는 사용자를 대상으로 연차 부여
            userWorkDays.forEach((userId, workDays) -> {
                if (workDays >= 15.0) { // 월 15일 이상 근무 시
                    log.info("사용자 ID: {}, 근무일수: {}. 연차 부여 대상입니다.", userId, workDays);
                    vacationServiceClient.grantMonthlyLeave(userId); // 변경
                } else {
                    log.info("사용자 ID: {}, 근무일수: {}. 연차 부여 대상이 아닙니다. (15일 미만)", userId, workDays);
                }
            });

            log.info("연차 부여 스케줄을 성공적으로 완료했습니다.");

        } catch (Exception e) {
            log.error("월별 연차 부여 스케줄 실행 중 오류가 발생했습니다.", e);
        }
    }
}
