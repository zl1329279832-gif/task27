package com.training.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private int code = 500;

    private String message;

    public BusinessException(String message) {
        super(message);
        this.message = message;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}
