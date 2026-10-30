package com.community.groupbuy.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter; // 注入你刚写的拦截器

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 开发环境：允许前端(5173/5174)跨域直连后端 8080，绕过本地代理对 /api 的干扰。
                // 生产由 Nginx 同源转发，此白名单仅含 localhost dev origin，对生产无影响。
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. 放行登录、注册接口
                        .requestMatchers("/api/auth/**").permitAll()
                        // 2. 放行 Knife4j 接口文档
                        .requestMatchers(
                                "/doc.html",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/webjars/**",
                                "/knife4j/**",
                                "/favicon.ico"
                        ).permitAll()
                        // 3. 重点：放行居民端首页未登录需要访问的只读接口 (B 和 C 的接口)
                        .requestMatchers(HttpMethod.GET,
                                "/api/activities/ongoing",
                                "/api/products/list",
                                "/api/pickup/points").permitAll()
                        // 4. 其他所有请求都需要登录认证
                        .anyRequest().authenticated()
                )
                // 把我们的 JWT 过滤器加在密码校验过滤器之前
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 白名单：仅放行开发用的 localhost 前端 origin。
     * 允许携带凭据（JWT 走 Authorization 请求头，浏览器跨域需 allowCredentials）。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.asList(
                "http://127.0.0.1:5173",
                "http://127.0.0.1:5174",
                "http://localhost:5173",
                "http://localhost:5174"
        ));
        cfg.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(Arrays.asList("*"));
        cfg.setExposedHeaders(Arrays.asList("Authorization"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}