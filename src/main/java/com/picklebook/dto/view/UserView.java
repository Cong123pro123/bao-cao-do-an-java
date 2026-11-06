package com.picklebook.dto.view;

import com.picklebook.entity.enums.RoleName;
import com.picklebook.entity.enums.UserStatus;
import com.picklebook.util.TimeUtils;

import java.time.LocalDateTime;

/**
 * Dữ liệu người dùng hiển thị trên giao diện (không lộ mật khẩu, không trả Entity).
 */
public class UserView {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private RoleName role;
    private String roleLabel;
    private UserStatus status;
    private String statusLabel;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    /**
     * Chữ viết tắt dùng làm avatar khi người dùng chưa tải ảnh.
     */
    public String getInitials() {
        if (fullName == null || fullName.isBlank()) {
            return "?";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        String first = parts[0].substring(0, 1);
        String last = parts[parts.length - 1].substring(0, 1);
        return (first + last).toUpperCase();
    }

    public boolean isAdmin() {
        return role == RoleName.ROLE_ADMIN;
    }

    public String getCreatedAtLabel() {
        return createdAt == null ? "" : TimeUtils.formatDate(createdAt.toLocalDate());
    }

    public String getLastLoginAtLabel() {
        return lastLoginAt == null ? "Chưa đăng nhập" : TimeUtils.formatDateTime(lastLoginAt);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public RoleName getRole() {
        return role;
    }

    public void setRole(RoleName role) {
        this.role = role;
    }

    public String getRoleLabel() {
        return roleLabel;
    }

    public void setRoleLabel(String roleLabel) {
        this.roleLabel = roleLabel;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
