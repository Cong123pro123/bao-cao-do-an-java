package com.picklebook.entity.enums;

/**
 * Mã các mẫu email hệ thống. Admin có thể sửa nội dung nhưng không đổi mã.
 */
public enum EmailTemplateCode {
    BOOKING_CONFIRMATION("Xác nhận đặt sân"),
    BOOKING_REMINDER("Nhắc lịch chơi"),
    BOOKING_CANCELLED("Hủy đơn đặt sân"),
    BOOKING_RESCHEDULED("Đổi lịch đặt sân"),
    PAYMENT_CONFIRMED("Xác nhận thanh toán"),
    PASSWORD_RESET("Đặt lại mật khẩu");

    private final String label;

    EmailTemplateCode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
