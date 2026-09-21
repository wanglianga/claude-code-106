package com.tcm.config;

import com.tcm.model.Role;
import com.tcm.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

public final class AuthUtils {

    private AuthUtils() {
    }

    public static User current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或登录已过期");
        }
        return user;
    }

    public static void requireRole(User user, Role... roles) {
        boolean ok = Arrays.stream(roles).anyMatch(r -> r == user.getRole());
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前角色无权执行该操作");
        }
    }
}
