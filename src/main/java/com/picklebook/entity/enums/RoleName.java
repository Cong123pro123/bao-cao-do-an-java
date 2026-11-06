package com.picklebook.entity.enums;

/**
 * Các vai trò trong hệ thống. Tên enum trùng với authority của Spring Security.
 */
public enum RoleName {
    ROLE_ADMIN("Quản trị viên"),
    ROLE_STAFF("Nhân viên"),
    ROLE_USER("Khách hàng");

    private final String label;

    RoleName(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
