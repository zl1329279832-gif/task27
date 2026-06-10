package com.training.enums;

public enum SubmissionStatus {

    IN_PROGRESS("IN_PROGRESS", "进行中"),
    SUBMITTED("SUBMITTED", "已提交"),
    TIMED_OUT("TIMED_OUT", "已超时"),
    AUTO_SUBMITTED("AUTO_SUBMITTED", "自动提交");

    private final String code;
    private final String description;

    SubmissionStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SubmissionStatus fromCode(String code) {
        for (SubmissionStatus value : SubmissionStatus.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown SubmissionStatus code: " + code);
    }
}
