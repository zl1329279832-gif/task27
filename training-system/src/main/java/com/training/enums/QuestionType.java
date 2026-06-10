package com.training.enums;

public enum QuestionType {

    SINGLE_CHOICE("SINGLE_CHOICE", "单选题"),
    MULTI_CHOICE("MULTI_CHOICE", "多选题"),
    TRUE_FALSE("TRUE_FALSE", "判断题"),
    FILL_BLANK("FILL_BLANK", "填空题");

    private final String code;
    private final String description;

    QuestionType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static QuestionType fromCode(String code) {
        for (QuestionType value : QuestionType.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown QuestionType code: " + code);
    }
}
