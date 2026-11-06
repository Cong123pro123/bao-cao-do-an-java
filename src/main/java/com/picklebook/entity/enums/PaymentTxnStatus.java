package com.picklebook.entity.enums;

public enum PaymentTxnStatus {
    PENDING("Đang chờ"),
    SUCCESS("Thành công"),
    FAILED("Thất bại"),
    REFUNDED("Đã hoàn tiền");

    private final String label;

    PaymentTxnStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
