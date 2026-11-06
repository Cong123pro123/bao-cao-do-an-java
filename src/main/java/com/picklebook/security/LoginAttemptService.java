package com.picklebook.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Giới hạn số lần đăng nhập sai theo email (lưu trong bộ nhớ).
 * Phù hợp cho một instance; khi chạy nhiều instance nên thay bằng Redis.
 */
@Component
public class LoginAttemptService {

    private record Attempt(int count, LocalDateTime lockedUntil) {
    }

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final int lockMinutes;

    public LoginAttemptService(@Value("${app.security.max-login-attempts:5}") int maxAttempts,
                               @Value("${app.security.lock-minutes:15}") int lockMinutes) {
        this.maxAttempts = maxAttempts;
        this.lockMinutes = lockMinutes;
    }

    public boolean isBlocked(String email) {
        if (email == null) {
            return false;
        }
        Attempt attempt = attempts.get(normalize(email));
        if (attempt == null) {
            return false;
        }
        if (attempt.lockedUntil() != null && attempt.lockedUntil().isAfter(LocalDateTime.now())) {
            return true;
        }
        if (attempt.lockedUntil() != null) {
            attempts.remove(normalize(email));
        }
        return false;
    }

    public void loginFailed(String email) {
        if (email == null) {
            return;
        }
        String key = normalize(email);
        Attempt current = attempts.get(key);
        int count = current == null ? 1 : current.count() + 1;
        LocalDateTime lockedUntil = count >= maxAttempts ? LocalDateTime.now().plusMinutes(lockMinutes) : null;
        attempts.put(key, new Attempt(count, lockedUntil));
    }

    public void loginSucceeded(String email) {
        if (email != null) {
            attempts.remove(normalize(email));
        }
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
