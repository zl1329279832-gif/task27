package com.training.enums;

public enum FileType {

    VIDEO("VIDEO", "视频"),
    PDF("PDF", "PDF文档"),
    DOC("DOC", "Word文档"),
    PPT("PPT", "PPT演示文稿"),
    LINK("LINK", "外部链接");

    private final String code;
    private final String description;

    FileType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static FileType fromCode(String code) {
        for (FileType value : FileType.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown FileType code: " + code);
    }
}
