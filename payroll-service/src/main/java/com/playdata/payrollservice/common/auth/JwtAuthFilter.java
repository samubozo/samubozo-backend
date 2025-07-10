package com.playdata.payrollservice.common.auth;



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
import java.util.Enumeration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            log.info("요청 헤더: {} = {}", headerName, request.getHeader(headerName));
        }

        String userEmail = request.getHeader("X-User-Email");
        String userRole = request.getHeader("X-User-Role");
        String employeeNoStr = request.getHeader("X-User-Employee-No"); // employeeNo 헤더 추가
        Long employeeNo = null;

        log.info("userEmail:{} userRole:{}", userEmail, userRole);

        if (userEmail != null && !userEmail.isEmpty() &&
                userRole != null && !userRole.isEmpty() &&
                employeeNoStr != null && !employeeNoStr.isEmpty()) {

            try {
                log.info("employeeNoStr: '{}'", employeeNoStr); // ✅ 디버깅 로그
                employeeNo = Long.parseLong(employeeNoStr);
                log.info("employeeNo 파싱 성공: {}", employeeNo); // ✅ 성공 로그
                Role roleEnum = Role.valueOf(userRole.toUpperCase()); // 안전하게 변환

                List<SimpleGrantedAuthority> authorityList = List.of(
                        new SimpleGrantedAuthority("ROLE_" + roleEnum.name())
                );

                Authentication auth = new UsernamePasswordAuthenticationToken(
                        new TokenUserInfo(userEmail, roleEnum, employeeNo),
                        null,
                        authorityList
                );

                SecurityContextHolder.getContext().setAuthentication(auth);
                log.info("[VacationService JwtAuthFilter] 인증 성공: {}", userEmail);
            } catch (Exception e) {
                log.warn("[VacationService JwtAuthFilter] Header parsing error: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid user info");
                return;
            }
        } else {
            log.warn("[VacationService JwtAuthFilter] Missing headers");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing authentication info");
            return;
        }
        filterChain.doFilter(request, response);
    }

}










