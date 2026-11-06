package com.picklebook.entity.enums;

public enum QuestionType {
    TEXT("Văn bản ngắn"),
    NUMBER("Số"),
    SELECT("Chọn một"),
    CHECKBOX("Hộp kiểm");

    private final String label;

    QuestionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
