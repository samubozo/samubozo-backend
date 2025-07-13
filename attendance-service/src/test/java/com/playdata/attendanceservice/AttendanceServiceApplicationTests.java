package com.playdata.attendanceservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource; // 임포트 추가

@SpringBootTest
@TestPropertySource(properties = { // 추가
    "standard.checkin.time=09:00",
    "standard.checkout.time=18:00"
})
class AttendanceServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
