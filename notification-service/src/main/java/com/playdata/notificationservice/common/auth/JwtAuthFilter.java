package com.playdata.notificationservice.common.auth;

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
    protected boolean shouldNotFilterAsyncDispatch() {
        return false; // async 디스패치(예: SSE)에도 필터 실행
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false; // 에러 디스패치에도 필터 실행(권장)
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String userEmail = request.getHeader("X-User-Email");
        String userRole = request.getHeader("X-User-Role");
        String employeeNoStr = request.getHeader("X-User-Employee-No");

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