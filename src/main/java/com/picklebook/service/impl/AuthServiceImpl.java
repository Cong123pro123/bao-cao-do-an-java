package com.picklebook.service.impl;

import com.picklebook.dto.request.RegisterRequest;
import com.picklebook.entity.PasswordResetToken;
import com.picklebook.entity.Role;
import com.picklebook.entity.User;
import com.picklebook.entity.enums.AuditAction;
import com.picklebook.entity.enums.EmailTemplateCode;
import com.picklebook.entity.enums.RoleName;
import com.picklebook.entity.enums.UserStatus;
import com.picklebook.exception.BusinessException;
import com.picklebook.repository.PasswordResetTokenRepository;
import com.picklebook.repository.RoleRepository;
import com.picklebook.repository.UserRepository;
import com.picklebook.security.TokenService;
import com.picklebook.service.AuditLogService;
import com.picklebook.service.AuthService;
import com.picklebook.service.EmailService;
import com.picklebook.service.SettingService;
import com.picklebook.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

/**
 * Triển khai đăng ký và luồng quên/đặt lại mật khẩu.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final int RESET_TOKEN_MINUTES = 30;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final SettingService settingService;
    private final AuditLogService auditLogService;

    public AuthServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordResetTokenRepository tokenRepository,
                           PasswordEncoder passwordEncoder,
                           TokenService tokenService,
                           EmailService emailService,
                           SettingService settingService,
                           AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.settingService = settingService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        if (!request.isPasswordConfirmed()) {
            throw new BusinessException("Mật khẩu xác nhận không khớp");
        }
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email này đã được sử dụng");
        }
        Role role = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new BusinessException("Hệ thống chưa cấu hình vai trò người dùng"));

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPhone(request.getPhone().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        auditLogService.log(AuditAction.CREATE, "User", user.getId(),
                "Đăng ký tài khoản mới: " + email);
        log.info("Đăng ký tài khoản mới: {}", email);
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        String normalized = normalizeEmail(email);
        userRepository.findByEmailIgnoreCase(normalized).ifPresent(user -> {
            tokenRepository.deleteAllByUserId(user.getId());

            String rawToken = tokenService.generateToken();
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(tokenService.hash(rawToken));
            token.setExpiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_MINUTES));
            tokenRepository.save(token);

            String resetUrl = settingService.get("app.base-url", "http://localhost:8080")
                    + "/dat-lai-mat-khau?token=" + rawToken;
            emailService.sendTemplate(EmailTemplateCode.PASSWORD_RESET, user.getEmail(), Map.of(
                    "customerName", user.getFullName(),
                    "resetUrl", resetUrl
            ));
            log.info("Đã tạo yêu cầu đặt lại mật khẩu cho {}", user.getEmail());
        });
    }

    @Override
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException("Liên kết đặt lại mật khẩu không hợp lệ");
        }
        String hash = tokenService.hash(rawToken);
        PasswordResetToken token = tokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException("Liên kết đặt lại mật khẩu không hợp lệ"));

        if (!token.isUsable(TimeUtils.now())) {
            throw new BusinessException("Liên kết đặt lại mật khẩu đã hết hạn hoặc đã được sử dụng");
        }
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsedAt(TimeUtils.now());
        tokenRepository.save(token);
        tokenRepository.deleteAllByUserId(user.getId());

        auditLogService.log(AuditAction.UPDATE, "User", user.getId(),
                "Đặt lại mật khẩu qua email: " + user.getEmail());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
