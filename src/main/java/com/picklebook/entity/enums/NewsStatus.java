package com.picklebook.entity.enums;

public enum NewsStatus {
    DRAFT("Bản nháp"),
    PUBLISHED("Đã đăng");

    private final String label;

    NewsStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
