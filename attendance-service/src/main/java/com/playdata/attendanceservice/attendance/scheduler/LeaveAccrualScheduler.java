package com.playdata.attendanceservice.attendance.scheduler;

import com.playdata.attendanceservice.attendance.dto.MonthlyWorkDaysResponse;
import com.playdata.attendanceservice.attendance.entity.WorkStatusType;
import com.playdata.attendanceservice.attendance.repository.AttendanceRepository;
import com.playdata.attendanceservice.attendance.service.VacationService;
import com.playdata.attendanceservice.client.HrServiceClient;
import com.playdata.attendanceservice.client.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 정기적으로 연차를 부여하는 스케줄러 클래스입니다.
 */
@Slf4j // 로그 사용을 위한 Lombok 어но테이션
@Component // Spring의 컴포넌트로 등록
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 생성
public class LeaveAccrualScheduler {

    private final AttendanceRepository attendanceRepository;
    private final VacationService vacationService;
    private final HrServiceClient hrServiceClient; // HR 서비스와 통신하기 위한 Feign 클라이언트

    /**
     * 매일 새벽 1시에 실행되어, 입사 1주년이 된 사용자에게 15일의 연차를 부여합니다.
     * cron = "초 분 시 일 월 요일"
     * 예: "0 0 1 * * ?" -> 매일 새벽 1시 0분 0초에 실행
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void grantFirstYearAnnualLeave() {
        LocalDate today = LocalDate.now();
        log.info("{} 기준, 입사 1주년 연차 부여 스케줄을 시작합니다.", today);

        try {
            // 1. HR 서비스에서 오늘이 입사 1주년인 사용자 목록을 조회합니다.
            List<UserDto> users = hrServiceClient.getUsersWithFirstAnniversary(today);
            log.info("총 {}명의 입사 1주년 대상자가 조회되었습니다.", users.size());

            // 2. 대상자들에게 15일의 연차를 부여합니다.
            users.forEach(user -> {
                log.info("사용자 ID: {}. 1주년 연차(15일) 부여 대상입니다.", user.getUserId());
                vacationService.grantAnnualLeave(Long.valueOf(user.getUserId()), 15);
            });

            log.info("입사 1주년 연차 부여 스케줄을 성공적으로 완료했습니다.", today);

        } catch (Exception e) {
            log.error("입사 1주년 연차 부여 스케줄 실행 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 매월 1일 새벽 2시에 실행되어, 지난달 근무일수를 기준으로 연차를 부여합니다.
     * cron = "초 분 시 일 월 요일"
     * 예: "0 0 2 1 * ?" -> 매월 1일 새벽 2시 0분 0초에 실행
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void accrueMonthlyLeave() {
        // 지난달 계산
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        log.info("{}년 {}월 근무 기록을 기준으로 연차 부여 스케줄을 시작합니다.", lastMonth.getYear(), lastMonth.getMonthValue());

        // 1. 지난달 근무일수 데이터 조회
        //    근무일로 인정할 상태(정상, 지각) 목록을 파라미터로 전달합니다.
        List<WorkStatusType> workStatuses = List.of(WorkStatusType.REGULAR, WorkStatusType.LATE);
        List<MonthlyWorkDaysResponse> workDaysData = attendanceRepository.countWorkDaysByMonth(
                lastMonth.getYear(),
                lastMonth.getMonthValue(),
                workStatuses
        );

        log.info("총 {}명의 근무 기록이 조회되었습니다.", workDaysData.size());

        // 2. 연차 부여 조건(예: 15일 이상 근무)을 충족하는 사용자를 대상으로 연차 부여
        workDaysData.stream()
                .filter(data -> data.getWorkDayCount() >= 15) // 월 15일 이상 근무 시
                .forEach(data -> {
                    log.info("사용자 ID: {}, 근무일수: {}. 연차 부여 대상입니다.", data.getUserId(), data.getWorkDayCount());
                    vacationService.grantMonthlyLeave(data.getUserId());
                });

        log.info("연차 부여 스케줄을 성공적으로 완료했습니다.");
    }
}
