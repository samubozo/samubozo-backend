package com.playdata.attendanceservice.common.configs;

import com.playdata.attendanceservice.common.auth.JwtAuthFilter;
import com.playdata.attendanceservice.common.exception.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 권한 검사를 컨트롤러의 메서드에서 전역적으로 수행하기 위한 설정.
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable());

        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        //여기에 권한 없이 접근해야할 URL을 명시해주세요. "/actuator/**" 는 건드시면 안됩니다.
        http.authorizeHttpRequests(auth -> {
            auth
                    .requestMatchers("/actuator/**",
                            "/swagger-ui.html", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**").permitAll() // actuator 엔드포인트는 허용
                    .anyRequest().authenticated(); // 그 외 모든 요청은 인증 필요
        });

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        http.exceptionHandling(exception -> {
            exception.authenticationEntryPoint(customAuthenticationEntryPoint);
        });

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}









