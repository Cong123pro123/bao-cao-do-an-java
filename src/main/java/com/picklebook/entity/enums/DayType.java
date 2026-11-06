package com.picklebook.entity.enums;

public enum DayType {
    WEEKDAY("Ngày thường"),
    WEEKEND("Cuối tuần"),
    HOLIDAY("Ngày lễ");

    private final String label;

    DayType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
