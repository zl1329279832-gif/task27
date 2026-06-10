package com.training.enums;

public enum CertificateStatus {

    VALID("VALID", "有效"),
    REVOKED("REVOKED", "已撤销");

    private final String code;
    private final String description;

    CertificateStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static CertificateStatus fromCode(String code) {
        for (CertificateStatus value : CertificateStatus.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown CertificateStatus code: " + code);
    }
}
