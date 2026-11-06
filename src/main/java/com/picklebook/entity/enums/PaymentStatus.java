package com.picklebook.entity.enums;

public enum PaymentStatus {
    UNPAID("Chưa thanh toán"),
    DEPOSIT_PAID("Đã cọc"),
    PAID("Đã thanh toán"),
    REFUNDED("Đã hoàn tiền");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
