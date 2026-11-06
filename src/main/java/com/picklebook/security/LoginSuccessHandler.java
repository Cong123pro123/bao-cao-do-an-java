package com.picklebook.security;

import com.picklebook.entity.enums.AuditAction;
import com.picklebook.repository.UserRepository;
import com.picklebook.service.AuditLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Ghi nhận thời điểm đăng nhập và audit log, sau đó chuyển về trang trước đó
 * (hoặc trang chủ nếu không có).
 */
@Component
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public LoginSuccessHandler(UserRepository userRepository, AuditLogService auditLogService,
                               RequestCache requestCache) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        setRequestCache(requestCache);
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(false);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof SecurityUser securityUser) {
            userRepository.findById(securityUser.getId()).ifPresent(user -> {
                user.setLastLoginAt(LocalDateTime.now());
                userRepository.save(user);
            });
            auditLogService.log(AuditAction.LOGIN, "User", securityUser.getId(),
                    "Đăng nhập thành công: " + securityUser.getEmail());
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
