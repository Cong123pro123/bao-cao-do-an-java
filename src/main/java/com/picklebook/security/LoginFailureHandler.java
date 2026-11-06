package com.picklebook.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Đếm số lần đăng nhập sai và chuyển về trang đăng nhập kèm mã lỗi tương ứng.
 */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(LoginFailureHandler.class);

    private final LoginAttemptService loginAttemptService;

    public LoginFailureHandler(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String email = request.getParameter("email");
        loginAttemptService.loginFailed(email);
        log.warn("Đăng nhập thất bại cho email={} từ IP={}: {}",
                email, request.getRemoteAddr(), exception.getMessage());

        String target = "/dang-nhap?error";
        if (exception instanceof DisabledException) {
            target = "/dang-nhap?disabled";
        } else if (exception instanceof LockedException) {
            target = "/dang-nhap?locked";
        }
        response.sendRedirect(request.getContextPath() + target);
    }
}
