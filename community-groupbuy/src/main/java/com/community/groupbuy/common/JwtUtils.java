package com.community.groupbuy.common;

import com.community.groupbuy.config.JwtUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {
    // 密钥必须足够长（至少32个字符），用于 HS256 加密
    private static final String SECRET_STRING = "community-groupbuy-secret-key-for-jwt-2026";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));
    // 过期时间 24 小时 (毫秒)
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000;

    // 生成 Token
    public String generateToken(JwtUser jwtUser) {
        return Jwts.builder()
                .subject(jwtUser.getUsername()) // 新版改名了
                .claim("id", jwtUser.getId())
                .claim("role", jwtUser.getRole())
                .issuedAt(new Date())           // 新版改名了
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 新版改名了
                .signWith(SECRET_KEY)           // 新版不需要传算法了
                .compact();
    }

    // 解析 Token，获取 JwtUser
    public JwtUser parseToken(String token) {
        try {
            Claims claims = Jwts.parser()       // 新版用 parser() 替代了 parserBuilder()
                    .verifyWith(SECRET_KEY)     // 新版用 verifyWith 替代了 setSigningKey
                    .build()
                    .parseSignedClaims(token)   // 新版用 parseSignedClaims 替代了 parseClaimsJws
                    .getPayload();              // 新版用 getPayload 替代了 getBody

            Long id = claims.get("id", Number.class).longValue();
            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            return new JwtUser(id, username, role, null);
        } catch (Exception e) {
            return null; // Token 无效或过期
        }
    }
}