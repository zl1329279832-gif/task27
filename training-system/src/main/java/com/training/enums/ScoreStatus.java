package com.training.enums;

public enum ScoreStatus {

    PENDING("PENDING", "待审核"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "未通过");

    private final String code;
    private final String description;

    ScoreStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ScoreStatus fromCode(String code) {
        for (ScoreStatus value : ScoreStatus.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown ScoreStatus code: " + code);
    }
}
