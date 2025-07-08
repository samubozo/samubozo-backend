package com.playdata.approvalservice.common.auth;

import com.playdata.approvalservice.common.auth.TokenUserInfo;
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

        log.info("[VacationService JwtAuthFilter] Received Headers - Email: {}, Role: {}, EmpNoStr: {}",
                userEmail, userRole, employeeNoStr);

        Long employeeNo = null;
        boolean headersPresentAndValid = false; // 헤더 존재 및 유효성 플래그

        if (userEmail != null && !userEmail.isEmpty() &&
                userRole != null && !userRole.isEmpty() &&
                employeeNoStr != null && !employeeNoStr.isEmpty()) {
            try {
                employeeNo = Long.parseLong(employeeNoStr);
                headersPresentAndValid = true; // 모든 헤더가 존재하고 유효함
            } catch (NumberFormatException e) {
                log.warn("[VacationService JwtAuthFilter] Invalid X-User-Employee-No header format: '{}'. Error: {}", employeeNoStr, e.getMessage());
            }
        } else {
            log.warn("[VacationService JwtAuthFilter] Missing or empty X-User-* headers. Email={}, Role={}, EmpNoStr={}",
                    userEmail, userRole, employeeNoStr);
        }

        if (headersPresentAndValid && employeeNo != null) { // employeeNo가 null이 아닌지 다시 확인
            List<SimpleGrantedAuthority> authorityList = new ArrayList<>();
            authorityList.add(new SimpleGrantedAuthority("ROLE_" + userRole));

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    new TokenUserInfo(userEmail, userRole, employeeNo),
                    null,
                    authorityList
            );

            SecurityContextHolder.getContext().setAuthentication(auth);
            log.info("[VacationService JwtAuthFilter] Authentication successful for user: {}", userEmail);

        } else {
            log.warn("[VacationService JwtAuthFilter] Authentication failed: Missing or invalid required headers.");
            // 인증이 필수인 경우, 여기서 401 Unauthorized 응답을 보냅니다.
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required: Missing or invalid user information.");
            return; // 다음 필터 체인으로 넘어가지 않고 요청 종료
        }

        filterChain.doFilter(request, response);
    }
}
