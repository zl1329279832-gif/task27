package com.training.enums;

public enum ExamStatus {

    DRAFT("DRAFT", "草稿"),
    PUBLISHED("PUBLISHED", "已发布"),
    CLOSED("CLOSED", "已关闭");

    private final String code;
    private final String description;

    ExamStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ExamStatus fromCode(String code) {
        for (ExamStatus value : ExamStatus.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown ExamStatus code: " + code);
    }
}
