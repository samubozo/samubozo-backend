package com.playdata.hrservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql; // Sql 어노테이션 임포트

@SpringBootTest
@Sql("/data.sql") // 테스트 실행 전에 data.sql을 로드하도록 설정
class HrServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
