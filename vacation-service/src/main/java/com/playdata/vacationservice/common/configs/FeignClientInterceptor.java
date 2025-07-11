package com.playdata.vacationservice.common.configs;

import com.playdata.vacationservice.common.auth.TokenUserInfo;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Feign Client 요청에 인증 헤더를 추가하는 인터셉터입니다.
 * 현재 SecurityContextHolder에 저장된 사용자 정보를 추출하여
 * X-User-Email, X-User-Role, X-User-Employee-No 헤더로 Feign 요청에 추가합니다.
 */
@Slf4j
public class FeignClientInterceptor implements RequestInterceptor {

    private static final String X_USER_EMAIL = "X-User-Email";
    private static final String X_USER_ROLE = "X-User-Role";
    private static final String X_USER_EMPLOYEE_NO = "X-User-Employee-No";

    @Override
    public void apply(RequestTemplate template) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof TokenUserInfo) {
            TokenUserInfo userInfo = (TokenUserInfo) authentication.getPrincipal();
            template.header(X_USER_EMAIL, userInfo.getEmail());
            template.header(X_USER_ROLE, userInfo.getHrRole());
            template.header(X_USER_EMPLOYEE_NO, String.valueOf(userInfo.getEmployeeNo()));
            log.info("VacationService FeignClientInterceptor: Added headers - Email: {}, Role: {}, EmployeeNo: {}",
                    userInfo.getEmail(), userInfo.getHrRole(), userInfo.getEmployeeNo());
        } else {
            log.warn("VacationService FeignClientInterceptor: No authentication information found in SecurityContextHolder or principal is not TokenUserInfo.");
        }
    }
}
