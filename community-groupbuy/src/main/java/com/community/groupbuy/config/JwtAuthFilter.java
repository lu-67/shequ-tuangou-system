package com.community.groupbuy.config;

import com.community.groupbuy.common.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 从请求头获取 Authorization
        String authHeader = request.getHeader("Authorization");

        // 2. 检查是否以 "Bearer " 开头
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            // 截取 Token
            String token = authHeader.substring(7);

            // 3. 解析 Token 获取用户信息
            JwtUser jwtUser = jwtUtils.parseToken(token);

            // 4. 如果解析成功，且当前 Spring Security 上下文中没有用户，则放入上下文
            if (jwtUser != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 封装权限，Spring Security 默认会在角色前加 ROLE_
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + jwtUser.getRole());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(jwtUser, null, Collections.singletonList(authority));

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 核心：将认证信息存入上下文，代表该用户已登录
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 5. 放行，进入下一个过滤器
        filterChain.doFilter(request, response);
    }
}