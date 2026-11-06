package com.picklebook.entity.enums;

public enum RecurringContractStatus {
    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    ACTIVE("Đang hoạt động"),
    COMPLETED("Hoàn thành"),
    CANCELLED("Đã hủy");

    private final String label;

    RecurringContractStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

