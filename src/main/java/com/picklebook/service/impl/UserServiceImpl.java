package com.picklebook.service.impl;

import com.picklebook.dto.request.AdminUserForm;
import com.picklebook.dto.request.ChangePasswordForm;
import com.picklebook.dto.request.ProfileForm;
import com.picklebook.dto.view.UserView;
import com.picklebook.entity.Role;
import com.picklebook.entity.User;
import com.picklebook.entity.enums.AuditAction;
import com.picklebook.entity.enums.RoleName;
import com.picklebook.entity.enums.UserStatus;
import com.picklebook.exception.BusinessException;
import com.picklebook.exception.ResourceNotFoundException;
import com.picklebook.repository.RoleRepository;
import com.picklebook.repository.UserRepository;
import com.picklebook.security.SecurityUtils;
import com.picklebook.service.AuditLogService;
import com.picklebook.service.UserService;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Triển khai nghiệp vụ người dùng.
 *
 * <p>Quy tắc an toàn: Admin không thể tự hạ quyền hoặc tự khóa tài khoản đang
 * đăng nhập; mật khẩu luôn được băm BCrypt; mật khẩu tạm chỉ trả về một lần
 * và không bao giờ ghi vào audit log.</p>
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("tài khoản", id));
    }

    @Override
    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public UserView getViewById(Long id) {
        return toView(getById(id));
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, ProfileForm form) {
        User user = getById(userId);
        String email = normalizeEmail(form.getEmail());
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
            throw new BusinessException("Email này đã được sử dụng bởi tài khoản khác");
        }
        user.setFullName(form.getFullName().trim());
        user.setEmail(email);
        user.setPhone(form.getPhone() == null ? null : form.getPhone().trim());
        userRepository.save(user);
        auditLogService.log(AuditAction.UPDATE, "User", user.getId(), "Cập nhật hồ sơ cá nhân");
        log.debug("Người dùng {} cập nhật hồ sơ", user.getId());
    }

    @Override
    @Transactional
    public void updateAvatar(Long userId, String avatarUrl) {
        User user = getById(userId);
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
        auditLogService.log(AuditAction.UPDATE, "User", user.getId(), "Cập nhật ảnh đại diện");
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordForm form) {
        if (!form.isPasswordConfirmed()) {
            throw new BusinessException("Mật khẩu xác nhận không khớp");
        }
        User user = getById(userId);
        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng");
        }
        if (passwordEncoder.matches(form.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("Mật khẩu mới không được trùng mật khẩu hiện tại");
        }
        user.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
        userRepository.save(user);
        auditLogService.log(AuditAction.UPDATE, "User", user.getId(), "Đổi mật khẩu");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserView> search(String keyword, RoleName role, UserStatus status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return userRepository.findAll(buildSpecification(keyword, role, status), pageable).map(this::toView);
    }

    @Override
    @Transactional
    public UserView create(AdminUserForm form) {
        String email = normalizeEmail(form.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email này đã được sử dụng");
        }
        if (form.getPassword() == null || form.getPassword().length() < 8) {
            throw new BusinessException("Mật khẩu phải có ít nhất 8 ký tự");
        }
        User user = new User();
        applyForm(user, form);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        userRepository.save(user);
        auditLogService.log(AuditAction.CREATE, "User", user.getId(),
                "Tạo tài khoản " + email + " với vai trò " + form.getRole());
        return toView(user);
    }

    @Override
    @Transactional
    public UserView update(Long id, AdminUserForm form) {
        User user = getById(id);
        String email = normalizeEmail(form.getEmail());
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new BusinessException("Email này đã được sử dụng bởi tài khoản khác");
        }
        boolean editingSelf = Objects.equals(SecurityUtils.currentUserId().orElse(null), id);
        if (editingSelf && form.getRole() != RoleName.ROLE_ADMIN) {
            throw new BusinessException("Không thể tự hạ quyền quản trị của tài khoản đang đăng nhập");
        }
        if (editingSelf && form.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("Không thể tự khóa tài khoản đang đăng nhập");
        }
        applyForm(user, form);
        user.setEmail(email);
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            if (form.getPassword().length() < 8) {
                throw new BusinessException("Mật khẩu phải có ít nhất 8 ký tự");
            }
            user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        }
        userRepository.save(user);
        auditLogService.log(AuditAction.UPDATE, "User", user.getId(), "Cập nhật tài khoản " + email);
        return toView(user);
    }

    @Override
    @Transactional
    public void setStatus(Long id, UserStatus status) {
        if (status == null) {
            throw new BusinessException("Trạng thái tài khoản không hợp lệ");
        }
        User user = getById(id);
        boolean editingSelf = Objects.equals(SecurityUtils.currentUserId().orElse(null), id);
        if (editingSelf && status != UserStatus.ACTIVE) {
            throw new BusinessException("Không thể tự khóa tài khoản đang đăng nhập");
        }
        user.setStatus(status);
        userRepository.save(user);
        auditLogService.log(AuditAction.STATUS_CHANGE, "User", user.getId(),
                "Đổi trạng thái tài khoản " + user.getEmail() + " thành " + status.getLabel());
    }

    @Override
    @Transactional
    public String resetPassword(Long id) {
        User user = getById(id);
        String temporaryPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        userRepository.save(user);
        auditLogService.log(AuditAction.UPDATE, "User", user.getId(),
                "Admin đặt lại mật khẩu cho " + user.getEmail());
        log.info("Admin đã đặt lại mật khẩu cho tài khoản {}", user.getEmail());
        return temporaryPassword;
    }

    private Specification<User> buildSpecification(String keyword, RoleName role, UserStatus status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String trimmed = keyword.trim();
                String like = "%" + trimmed.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), like),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), like),
                        criteriaBuilder.like(root.get("phone"), "%" + trimmed + "%")
                ));
            }
            if (role != null) {
                predicates.add(criteriaBuilder.equal(root.get("role").get("name"), role));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void applyForm(User user, AdminUserForm form) {
        user.setFullName(form.getFullName().trim());
        user.setPhone(form.getPhone() == null || form.getPhone().isBlank() ? null : form.getPhone().trim());
        user.setRole(resolveRole(form.getRole()));
        user.setStatus(form.getStatus() == null ? UserStatus.ACTIVE : form.getStatus());
    }

    private Role resolveRole(RoleName roleName) {
        RoleName effectiveRole = roleName == null ? RoleName.ROLE_USER : roleName;
        return roleRepository.findByName(effectiveRole)
                .orElseThrow(() -> new BusinessException("Hệ thống chưa cấu hình vai trò " + effectiveRole));
    }

    private UserView toView(User user) {
        UserView view = new UserView();
        view.setId(user.getId());
        view.setFullName(user.getFullName());
        view.setEmail(user.getEmail());
        view.setPhone(user.getPhone());
        view.setAvatarUrl(user.getAvatarUrl());
        RoleName role = user.getRole() == null ? null : user.getRole().getName();
        view.setRole(role);
        view.setRoleLabel(role == null ? "" : role.getLabel());
        view.setStatus(user.getStatus());
        view.setStatusLabel(user.getStatus() == null ? "" : user.getStatus().getLabel());
        view.setLastLoginAt(user.getLastLoginAt());
        view.setCreatedAt(user.getCreatedAt());
        return view;
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder("Pb");
        for (int i = 0; i < 8; i++) {
            password.append(TEMP_PASSWORD_CHARS.charAt(RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        password.append("1!");
        return password.toString();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
