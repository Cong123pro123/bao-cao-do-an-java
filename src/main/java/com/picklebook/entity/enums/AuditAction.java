package com.picklebook.entity.enums;

public enum AuditAction {
    CREATE("Tạo mới"),
    UPDATE("Cập nhật"),
    DELETE("Xóa"),
    LOGIN("Đăng nhập"),
    LOGOUT("Đăng xuất"),
    STATUS_CHANGE("Đổi trạng thái"),
    CANCEL("Hủy đơn"),
    RESCHEDULE("Đổi lịch"),
    EXPORT("Xuất dữ liệu");

    private final String label;

    AuditAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
