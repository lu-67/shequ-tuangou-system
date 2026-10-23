package com.community.groupbuy.common;

import com.community.groupbuy.config.JwtUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 获取当前登录用户上下文
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static JwtUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof JwtUser user)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }

    public static Long getUserId() {
        return currentUser().getId();
    }

    public static String getUsername() {
        return currentUser().getUsername();
    }

    public static String getRole() {
        return currentUser().getRole();
    }
}
