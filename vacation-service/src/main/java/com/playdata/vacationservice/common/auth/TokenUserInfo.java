package com.playdata.vacationservice.common.auth;

import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails; // UserDetails 임포트

import java.util.Collection;
import java.util.Collections;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenUserInfo implements UserDetails { // UserDetails 인터페이스 구현

    private String email;
    private String hrRole;
    private Long employeeNo;

    // UserDetails 인터페이스 구현 메소드
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + hrRole));
    }

    @Override
    public String getPassword() {
        return null; // 비밀번호는 토큰에 포함되지 않으므로 null 반환
    }

    @Override
    public String getUsername() {
        return email; // 사용자 이름으로 email 사용
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
