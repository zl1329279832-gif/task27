package com.training.enums;

public enum CourseStatus {

    DRAFT("DRAFT", "草稿"),
    PUBLISHED("PUBLISHED", "已发布"),
    ARCHIVED("ARCHIVED", "已归档");

    private final String code;
    private final String description;

    CourseStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static CourseStatus fromCode(String code) {
        for (CourseStatus value : CourseStatus.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown CourseStatus code: " + code);
    }
}
