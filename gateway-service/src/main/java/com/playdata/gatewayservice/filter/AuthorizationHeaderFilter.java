package com.playdata.gatewayservice.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;


@Component
@Slf4j
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory {

    @Value("${jwt.secretKey}")
    private String secretKey;

    //여기에 권한 없이 접근해야할 URL을 명시해주세요.
    private final List<String> allowUrl = Arrays.asList(
            "/hr/users/signup"
            ,"/hr/user/feign/**"
            ,"/hr/positions"
            ,"/hr/departments"
            ,"/auth/login"
            ,"/auth/email-valid"
            ,"/auth/verify"
            ,"/auth/verify-code"
            ,"/attendance/**"
            ,"/auth/refresh"
    );

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            log.info("path: {}", path);
            AntPathMatcher antPathMatcher = new AntPathMatcher();


            boolean isAllowed
                    = allowUrl.stream()
                    .anyMatch(url -> antPathMatcher.match(url, path));
            log.info("url: {}", path);
            log.info("isAllowed: {}", isAllowed);

            if (isAllowed || path.startsWith("/actuator")) {

                log.info("gateway filter 통과!");
                return chain.filter(exchange);
            }

            String token = null;
            String authorizationHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

            // SSE 구독 요청의 경우 쿼리 파라미터에서 토큰 추출
            if (path.equals("/notifications/subscribe")) { // <-- 이 부분을 수정합니다.
                String queryToken = exchange.getRequest().getQueryParams().getFirst("token"); // "token" 파라미터 이름은 프론트엔드와 일치해야 합니다.
                if (queryToken != null && !queryToken.isEmpty()) {
                    token = queryToken;
                    log.info("SSE token from query parameter: {}", token);
                }
            } else if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                token = authorizationHeader.replace("Bearer ", "");
                log.info("Bearer token from Authorization header: {}", token);
            }

            if (token == null) {
                return onError(exchange, "Authorization token is missing or invalid", HttpStatus.UNAUTHORIZED);
            }

            Claims claims = validateJwt(token);
            if (claims == null) {
                return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
            }

            ServerHttpRequest request = exchange.getRequest()
                    .mutate()
                    .header("X-User-Email", claims.getSubject())
                    .header("X-User-Role", claims.get("role", String.class))
                    .header("X-User-Employee-No", claims.get("employeeNo", Long.class).toString())
                    .build();
            // 클레임 내용 로깅 (추가)
            log.info("[Gateway AuthFilter] Validated JWT Claims: Subject={}, Role={}, EmployeeNo={}",
                    claims.getSubject(), claims.get("role", String.class), claims.get("employeeNo", Long.class));
            return chain.filter(exchange.mutate().request(request).build());
        };
    }


    private Mono<Void> onError(ServerWebExchange exchange,
                               String msg, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        log.error(msg);

        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private Claims validateJwt(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return null;
        }
    }
}
