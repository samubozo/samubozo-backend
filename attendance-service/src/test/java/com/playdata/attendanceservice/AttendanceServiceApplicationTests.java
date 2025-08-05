package com.playdata.attendanceservice;

import com.playdata.attendanceservice.attendance.entity.Attendance;
import com.playdata.attendanceservice.workstatus.entity.WorkStatusType;
import com.playdata.attendanceservice.attendance.service.AttendanceService;
import com.playdata.attendanceservice.client.ApprovalServiceClient;
import com.playdata.attendanceservice.client.HrServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "standard.checkin.time=09:00",
    "standard.checkout.time=18:00"
})
class AttendanceServiceApplicationTests {

	@Autowired
	private AttendanceService attendanceService;

	@MockBean
	private ApprovalServiceClient approvalServiceClient;

	@MockBean
	private HrServiceClient hrServiceClient;

	@BeforeEach
	void setUp() {
		// 각 테스트 전에 Mockito를 초기화하여 Mock 객체의 행동을 재설정합니다.
		Mockito.reset(approvalServiceClient, hrServiceClient);
	}

	@Test
	void contextLoads() {
	}

	@DisplayName("휴가가 없는 경우 출근 기록이 정상적으로 처리되어야 한다")
	@Test
	void recordCheckIn_noLeave() {
		// Given
		Long userId = 1L;
		String ipAddress = "127.0.0.1";
		LocalDateTime checkInTime = LocalDateTime.of(2025, 7, 15, 9, 0, 0);

		// Mocking: HR 서비스에서 승인된 외부 일정이 없음을 가정
		Mockito.when(hrServiceClient.getApprovedExternalScheduleType(
				Mockito.anyLong(),
				Mockito.anyString()))
				.thenReturn(null);

		// Mocking: 승인된 휴가가 없음을 가정
		Mockito.when(approvalServiceClient.getApprovedLeaveType(
				Mockito.eq(userId),
				Mockito.eq(checkInTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE))))
				.thenReturn(null);

		// When
		Attendance attendance = attendanceService.recordCheckIn(userId, ipAddress, checkInTime);

		// Then
		assertThat(attendance).isNotNull();
		assertThat(attendance.getUserId()).isEqualTo(userId);
		assertThat(attendance.getWorkStatus().getStatusType()).isEqualTo(WorkStatusType.REGULAR);
	}

	@DisplayName("오전 반차 휴가가 있는 경우 출근 기록 시 WorkStatusType이 HALF_DAY_LEAVE로 설정되어야 한다")
	@Test
	void recordCheckIn_halfDayLeaveAm() {
		// Given
		Long userId = 2L;
		String ipAddress = "127.0.0.1";
		LocalDateTime checkInTime = LocalDateTime.of(2025, 7, 15, 9, 0, 0);

		// Mocking: HR 서비스에서 승인된 외부 일정이 없음을 가정
		Mockito.when(hrServiceClient.getApprovedExternalScheduleType(
				Mockito.anyLong(),
				Mockito.anyString()))
				.thenReturn(null);

		// Mocking: 오전 반차 휴가가 있음을 가정
		Mockito.when(approvalServiceClient.getApprovedLeaveType(
				Mockito.eq(userId),
				Mockito.eq(checkInTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE))))
				.thenReturn("HALF_DAY_LEAVE_AM");

		// When
		Attendance attendance = attendanceService.recordCheckIn(userId, ipAddress, checkInTime);

		// Then
		assertThat(attendance).isNotNull();
		assertThat(attendance.getUserId()).isEqualTo(userId);
		assertThat(attendance.getWorkStatus().getStatusType()).isEqualTo(WorkStatusType.HALF_DAY_LEAVE);
	}
}
