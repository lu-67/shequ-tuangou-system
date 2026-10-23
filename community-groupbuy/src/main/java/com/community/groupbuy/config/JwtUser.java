package com.community.groupbuy.config;

import com.community.groupbuy.common.Constants;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * JWT 认证主体（承载用户基本信息，供 SecurityUtils 读取）
 */
@Getter
public class JwtUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String role;
    private final String password;

    public JwtUser(Long id, String username, String role, String password) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.password = password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
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
