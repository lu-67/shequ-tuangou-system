package com.community.groupbuy.common;

import com.community.groupbuy.config.JwtUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M0 JWT 工具类单元测试（不启动 Spring 容器）。
 *
 * 覆盖：生成-解析往返一致、非法 token 返回 null、null token 返回 null、过期 token 返回 null。
 */
class JwtUtilsTest {

    private final JwtUtils jwtUtils = new JwtUtils();

    @Test
    void generateAndParse_roundtrip() {
        JwtUser user = new JwtUser(1L, "admin", "ADMIN", "N/A");
        String token = jwtUtils.generateToken(user);

        JwtUser parsed = jwtUtils.parseToken(token);
        assertThat(parsed).isNotNull();
        assertThat(parsed.getUsername()).isEqualTo("admin");
        assertThat(parsed.getId()).isEqualTo(1L);
        assertThat(parsed.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void parseToken_invalid_returnsNull() {
        assertThat(jwtUtils.parseToken("not-a-valid-token")).isNull();
    }

    @Test
    void parseToken_null_returnsNull() {
        assertThat(jwtUtils.parseToken(null)).isNull();
    }

    /** 过期 token 应解析失败并返回 null（与线上 24h 过期语义一致） */
    @Test
    void parseToken_expired_returnsNull() {
        // 仅测试用：复用与 JwtUtils 相同的密钥常量构造一个已过期的 token
        SecretKey key = Keys.hmacShaKeyFor(
                "community-groupbuy-secret-key-for-jwt-2026".getBytes(StandardCharsets.UTF_8));
        String expired = Jwts.builder()
                .subject("admin")
                .claim("id", 1L)
                .claim("role", "ADMIN")
                .issuedAt(new Date(System.currentTimeMillis() - 2L * 24 * 3600 * 1000))
                .expiration(new Date(System.currentTimeMillis() - 24L * 3600 * 1000))
                .signWith(key)
                .compact();

        assertThat(jwtUtils.parseToken(expired)).isNull();
    }
}
