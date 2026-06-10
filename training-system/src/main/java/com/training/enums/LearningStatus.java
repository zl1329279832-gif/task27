package com.training.enums;

public enum LearningStatus {

    NOT_STARTED("NOT_STARTED", "未开始"),
    IN_PROGRESS("IN_PROGRESS", "进行中"),
    COMPLETED("COMPLETED", "已完成");

    private final String code;
    private final String description;

    LearningStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static LearningStatus fromCode(String code) {
        for (LearningStatus value : LearningStatus.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown LearningStatus code: " + code);
    }
}
