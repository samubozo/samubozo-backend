package com.playdata.vacationservice.common.auth;

import com.playdata.vacationservice.common.auth.TokenUserInfo;
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

    // JwtTokenProvider와 CustomAuthenticationEntryPoint는 이 필터에서 직접 사용되지 않으므로 제거
    // private final JwtTokenProvider jwtTokenProvider;
    // private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

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
                log.warn("JwtAuthFilter: Invalid X-User-Employee-No header: {}", employeeNoStr);
            }
        }

        log.info("JwtAuthFilter: Received Headers - userEmail:{}, userRole:{}, employeeNo:{}", userEmail, userRole, employeeNo); // 로그 추가

        if (userEmail != null && userRole != null && employeeNo != null) {
            List<SimpleGrantedAuthority> authorityList = new ArrayList<>();
            authorityList.add(new SimpleGrantedAuthority("ROLE_" + userRole));

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    new TokenUserInfo(userEmail, userRole, employeeNo),
                    "",
                    authorityList
            );

            SecurityContextHolder.getContext().setAuthentication(auth);
            log.info("JwtAuthFilter: SecurityContextHolder updated for user - {}", userEmail); // 로그 추가

        } else {
            log.warn("JwtAuthFilter: Missing X-User-* headers. userEmail: {}, userRole: {}, employeeNo: {}", userEmail, userRole, employeeNo); // 로그 추가
        }
        filterChain.doFilter(request, response);

    }
}










