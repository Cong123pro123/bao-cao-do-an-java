package com.picklebook.entity.enums;

public enum PaymentMethod {
    CASH("Thanh toán tại sân"),
    BANK_TRANSFER("Chuyển khoản"),
    ONLINE("Thanh toán online");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
