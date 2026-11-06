package com.picklebook.entity.enums;

public enum BlockType {
    HOLIDAY("Ngày lễ"),
    MAINTENANCE("Bảo trì"),
    EVENT("Giải đấu / sự kiện");

    private final String label;

    BlockType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
