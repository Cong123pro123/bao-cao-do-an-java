package com.picklebook.entity.enums;

public enum DiscountType {
    PERCENT("Phần trăm"),
    FIXED("Số tiền cố định");

    private final String label;

    DiscountType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
