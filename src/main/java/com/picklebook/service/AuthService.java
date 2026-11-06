package com.picklebook.service;

import com.picklebook.dto.request.RegisterRequest;

/**
 * Nghiệp vụ tài khoản: đăng ký, quên/đặt lại mật khẩu.
 */
public interface AuthService {

    void register(RegisterRequest request);

    /**
     * Gửi email đặt lại mật khẩu nếu email tồn tại. Luôn im lặng để tránh lộ
     * thông tin tài khoản (user enumeration).
     */
    void requestPasswordReset(String email);

    void resetPassword(String rawToken, String newPassword);
}
