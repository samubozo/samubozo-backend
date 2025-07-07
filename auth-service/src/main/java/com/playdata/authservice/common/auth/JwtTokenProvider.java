package com.playdata.authservice.common.auth;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;


@Component
public class JwtTokenProvider {

    @Value("${jwt.secretKey}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private int expiration;

    @Value("${jwt.secretKeyRt}")
    private String secretKeyRt;

    @Value("${jwt.expirationRt}")
    private int expirationRt;


    public String createToken(String email, String hrRole, Long employeeNo){
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("role", hrRole);
        claims.put("employeeNo", employeeNo);
        Date now = new Date();

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiration * 100 * 10000))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    public String createRefreshToken(String email, String hrRole, Long employeeNo){
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("role", hrRole);
        claims.put("employeeNo", employeeNo);
        Date now = new Date();

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationRt * 60 * 1000))
                .signWith(SignatureAlgorithm.HS256, secretKeyRt)
                .compact();
    }

    public TokenUserInfo validateAndGetTokenUserInfo(String token)
            throws Exception {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        System.out.println("claims = " + claims);

        return TokenUserInfo.builder()
                .email(claims.getSubject())
                .hrRole(claims.get("role", String.class))
                .build();
    }
}















