package com.playdata.vacationservice.common.auth;


import com.playdata.vacationservice.common.auth.TokenUserInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import java.time.Duration;
import java.util.Date;


@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secretKey}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private Duration expiration;

    @Value("${jwt.secretKeyRt}")
    private String secretKeyRt;

    @Value("${jwt.expirationRt}")
    private Duration expirationRt;


    public String createToken(String email, String hrRole, Long employeeNo) {
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("role", hrRole);
        claims.put("employeeNo", employeeNo);
        Date now = new Date();

        log.info("Access Token Expiration (millis): {}", expiration.toMillis()); // 추가
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiration.toMillis()))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    public String createRefreshToken(String email, String hrRole, Long employeeNo) {
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("role", hrRole);
        claims.put("employeeNo", employeeNo);
        Date now = new Date();

        log.info("Refresh Token Expiration (millis): {}", expirationRt.toMillis()); // 추가
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationRt.toMillis()))
                .signWith(SignatureAlgorithm.HS256, secretKeyRt)
                .compact();
    }

    public TokenUserInfo validateAndGetTokenUserInfo(String token) throws Exception {
        log.info("JwtTokenProvider: Validating token - {}", token); // 로그 추가
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            log.info("JwtTokenProvider: Token claims - Subject: {}, Role: {}, EmployeeNo: {}", // 로그 추가
                    claims.getSubject(), claims.get("role", String.class), claims.get("employeeNo", Long.class));

            return TokenUserInfo.builder()
                    .email(claims.getSubject())
                    .hrRole(claims.get("role", String.class))
                    .employeeNo(claims.get("employeeNo", Long.class))
                    .build();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.error("JwtTokenProvider: Token expired - {}", e.getMessage()); // 로그 추가
            throw new Exception("Token expired", e);
        } catch (io.jsonwebtoken.SignatureException e) {
            log.error("JwtTokenProvider: Invalid JWT signature - {}", e.getMessage()); // 로그 추가
            throw new Exception("Invalid JWT signature", e);
        } catch (Exception e) {
            log.error("JwtTokenProvider: Token validation failed with unexpected error - {}", e.getMessage(), e); // 로그 추가
            throw e;
        }
    }

    public TokenUserInfo validateRefreshTokenAndGetTokenUserInfo(String token) throws Exception {
        log.info("JwtTokenProvider: Validating refresh token - {}", token); // 로그 추가
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKeyRt)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            log.info("JwtTokenProvider: Refresh token claims - Subject: {}, Role: {}, EmployeeNo: {}", // 로그 추가
                    claims.getSubject(), claims.get("role", String.class), claims.get("employeeNo", Long.class));

            return TokenUserInfo.builder()
                    .email(claims.getSubject())
                    .hrRole(claims.get("role", String.class))
                    .employeeNo(claims.get("employeeNo", Long.class))
                    .build();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.error("JwtTokenProvider: Refresh token expired - {}", e.getMessage()); // 로그 추가
            throw new Exception("Refresh token expired", e);
        } catch (io.jsonwebtoken.SignatureException e) {
            log.error("JwtTokenProvider: Invalid JWT refresh token signature - {}", e.getMessage()); // 로그 추가
            throw new Exception("Invalid JWT refresh token signature", e);
        } catch (Exception e) {
            log.error("JwtTokenProvider: Refresh token validation failed with unexpected error - {}", e.getMessage(), e); // 로그 추가
            throw e;
        }
    }
}















