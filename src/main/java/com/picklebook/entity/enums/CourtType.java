package com.picklebook.entity.enums;

public enum CourtType {
    INDOOR("Trong nhà"),
    OUTDOOR("Ngoài trời");

    private final String label;

    CourtType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
