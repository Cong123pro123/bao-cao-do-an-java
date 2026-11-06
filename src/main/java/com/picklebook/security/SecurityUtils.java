package com.picklebook.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Tiện ích truy cập người dùng đang đăng nhập.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<SecurityUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUser securityUser) {
            return Optional.of(securityUser);
        }
        return Optional.empty();
    }

    public static Optional<Long> currentUserId() {
        return currentUser().map(SecurityUser::getId);
    }

    public static String currentEmail() {
        return currentUser().map(SecurityUser::getEmail).orElse("anonymous");
    }

    public static boolean isAdmin() {
        return currentUser().map(SecurityUser::isAdmin).orElse(false);
    }
}
