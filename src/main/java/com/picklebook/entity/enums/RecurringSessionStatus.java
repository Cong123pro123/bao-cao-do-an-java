package com.picklebook.entity.enums;

public enum RecurringSessionStatus {
    SCHEDULED("Đã lên lịch"),
    ATTENDED("Đã tham gia"),
    CANCELLED("Nghỉ buổi"),
    RESCHEDULED("Đã dời buổi");

    private final String label;

    RecurringSessionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

