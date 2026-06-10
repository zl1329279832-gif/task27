package com.training.enums;

public enum ClazzStatus {

    ACTIVE("ACTIVE", "进行中"),
    INACTIVE("INACTIVE", "未开始"),
    COMPLETED("COMPLETED", "已结束");

    private final String code;
    private final String description;

    ClazzStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ClazzStatus fromCode(String code) {
        for (ClazzStatus value : ClazzStatus.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown ClazzStatus code: " + code);
    }
}
