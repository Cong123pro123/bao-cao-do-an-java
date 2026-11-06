package com.picklebook.entity.enums;

public enum CourtStatus {
    ACTIVE("Đang hoạt động"),
    MAINTENANCE("Bảo trì"),
    HIDDEN("Đã ẩn");

    private final String label;

    CourtStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isBookable() {
        return this == ACTIVE;
    }
}
