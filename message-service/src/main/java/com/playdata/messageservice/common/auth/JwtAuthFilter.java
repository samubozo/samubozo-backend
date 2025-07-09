package com.playdata.messageservice.common.auth;

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

        String requestURI = request.getRequestURI();
        String userEmail = null;
        String userRole = null;
        String employeeNoStr = null;

        // SSE 구독 요청의 경우, 쿼리 파라미터에서 인증 정보 추출
        if (requestURI.contains("/notifications/subscribe")) {
            log.info("SSE connection detected. Reading auth info from query parameters.");
            userEmail = request.getParameter("userEmail");
            userRole = request.getParameter("userRole");
            employeeNoStr = request.getParameter("employeeNo");
        } else {
            // 그 외 모든 요청은 기존 방식대로 헤더에서 인증 정보 추출
            userEmail = request.getHeader("X-User-Email");
            userRole = request.getHeader("X-User-Role");
            employeeNoStr = request.getHeader("X-User-Employee-No");
        }

        Long employeeNo = null;
        if (employeeNoStr != null) {
            try {
                employeeNo = Long.parseLong(employeeNoStr);
            } catch (NumberFormatException e) {
                log.warn("Invalid employee number format: {}", employeeNoStr);
            }
        }

        log.info("userEmail:{} userRole:{} employeeNo:{}", userEmail, userRole, employeeNo);

        if (userEmail != null && userRole != null && employeeNo != null) {
            List<SimpleGrantedAuthority> authorityList = new ArrayList<>();
            authorityList.add(new SimpleGrantedAuthority("ROLE_" + userRole));

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    new TokenUserInfo(userEmail, userRole, employeeNo),
                    "",
                    authorityList
            );

            SecurityContextHolder.getContext().setAuthentication(auth);

            log.info("SecurityContext Authentication: {}", SecurityContextHolder.getContext().getAuthentication());
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.info("Authentication Principal: {}", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
                log.info("Authentication Authorities: {}", SecurityContextHolder.getContext().getAuthentication().getAuthorities());
                log.info("Authentication isAuthenticated: {}", SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
            }

        }
        filterChain.doFilter(request, response);

    }
}