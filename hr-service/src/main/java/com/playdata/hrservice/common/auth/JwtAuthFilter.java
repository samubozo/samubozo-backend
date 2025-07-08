package com.playdata.hrservice.common.auth; // 실제 HR Service의 패키지 경로 확인

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils; // StringUtils 임포트 추가
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String userEmail = request.getHeader("X-User-Email");
        String userRole = request.getHeader("X-User-Role");
        String employeeNoStr = request.getHeader("X-User-Employee-No");

        log.info("[HR Service JwtAuthFilter] Received Headers - Email: {}, Role: {}, EmpNoStr: {}",
                userEmail, userRole, employeeNoStr);

        Long employeeNo = null;
        // employeeNo 문자열이 존재하고 비어있지 않다면 Long으로 파싱 시도
        if (StringUtils.hasText(employeeNoStr)) { // StringUtils.hasText() 사용 권장
            try {
                employeeNo = Long.parseLong(employeeNoStr);
            } catch (NumberFormatException e) {
                log.warn("[HR Service JwtAuthFilter] Invalid X-User-Employee-No header format: '{}'. Error: {}", employeeNoStr, e.getMessage());
                // 파싱 실패 시 employeeNo는 null로 유지되어 아래 인증 실패 조건에 걸립니다.
            }
        }

        // 모든 필수 헤더 (userEmail, userRole, employeeNo)가 존재하고 유효한지 확인
        if (StringUtils.hasText(userEmail) && StringUtils.hasText(userRole) && employeeNo != null) {
            List<SimpleGrantedAuthority> authorityList = new ArrayList<>();
            // Spring Security는 "ROLE_" 접두사를 권한 이름에 요구합니다.
            authorityList.add(new SimpleGrantedAuthority("ROLE_" + userRole));

            // 인증 완료 처리: Spring Security의 Authentication 객체를 생성합니다.
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    // TokenUserInfo 생성자에 employeeNo를 포함하여 전달합니다.
                    // HR 서비스의 TokenUserInfo 클래스에도 employeeNo 필드가 있어야 합니다.
                    new TokenUserInfo(userEmail, userRole, employeeNo),
                    null, // 인증된 사용자의 비밀번호: JWT 인증에서는 보통 null 또는 빈 문자열로 선언합니다.
                    authorityList
            );

            // SecurityContextHolder에 인증 정보 객체를 등록합니다.
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.info("[HR Service JwtAuthFilter] Authentication successful for user: {}", userEmail);

        } else {
            // 필수 헤더 중 하나라도 누락되었거나 유효하지 않은 경우
            log.warn("[HR Service JwtAuthFilter] Authentication failed: Missing or invalid required headers. Email={}, Role={}, EmployeeNo={}",
                    userEmail, userRole, employeeNo);
            // **여기서 401 Unauthorized 응답을 반환하고 요청 처리를 중단합니다.**
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required: Missing or invalid user information in headers.");
            return; // <-- 이 부분이 중요합니다. 다음 필터 체인으로 넘어가지 않고 요청을 종료합니다.
        }

        // 인증에 성공했거나, 인증이 필요 없는 경로인 경우 다음 필터 체인을 진행합니다.
        filterChain.doFilter(request, response);
    }
}