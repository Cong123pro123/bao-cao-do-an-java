package com.picklebook.entity.enums;

public enum BookingStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    COMPLETED("Đã hoàn thành"),
    CANCELLED("Đã hủy"),
    NO_SHOW("Vắng mặt");

    private final String label;

    BookingStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isActive() {
        return this == PENDING || this == CONFIRMED;
    }

    public boolean isFinished() {
        return this == COMPLETED || this == CANCELLED || this == NO_SHOW;
    }
}
