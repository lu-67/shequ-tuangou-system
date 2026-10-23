package com.community.groupbuy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置。
 *
 * 当前状态（方案①执行后）：M0 用户与权限（D 同学负责）尚未实现，
 * 这里先用 **permitAll 占位**，保证工程可启动、M3 参团下单可联调。
 *
 * D 同学实现 M0 时需要：
 * 1. 新增 JwtAuthFilter（解析 Authorization: Bearer <token>，把 JwtUser 放入 SecurityContext）；
 * 2. 新增 DbUserDetailsService（按用户名从 sys_user 加载用户，密码用下方 PasswordEncoder 校验）；
 * 3. 把下方 anyRequest().permitAll() 改回 anyRequest().authenticated()，
 *    保留 /api/auth/** 与 Knife4j 相关路径 permitAll，并 addFilterBefore(jwtAuthFilter, ...)。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/doc.html",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/webjars/**",
                        "/knife4j/**",
                        "/favicon.ico"
                ).permitAll()
                // 临时占位：M0 实现后改为 authenticated()
                .anyRequest().permitAll()
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
