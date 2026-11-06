package com.picklebook.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Chặn sớm các yêu cầu đăng nhập khi email đang bị tạm khóa do sai quá nhiều lần.
 *
 * <p>Filter được khởi tạo trong {@link com.picklebook.config.SecurityConfig}
 * (không đánh dấu {@code @Component}) để tránh bị Spring Boot tự đăng ký lần
 * thứ hai như một servlet filter toàn cục.</p>
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final LoginAttemptService loginAttemptService;

    public LoginRateLimitFilter(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !"/dang-nhap".equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String email = request.getParameter("email");
        if (loginAttemptService.isBlocked(email)) {
            response.sendRedirect(request.getContextPath() + "/dang-nhap?locked");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
