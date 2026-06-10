package com.training.enums;

public enum RoleEnum {

    ADMIN("ADMIN", "管理员"),
    INSTRUCTOR("INSTRUCTOR", "讲师"),
    STUDENT("STUDENT", "学员"),
    AUDITOR("AUDITOR", "审核员");

    private final String code;
    private final String description;

    RoleEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static RoleEnum fromCode(String code) {
        for (RoleEnum value : RoleEnum.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown RoleEnum code: " + code);
    }
}
