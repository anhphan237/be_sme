package com.sme.be_sme.shared.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private final String code;
    public AppException(String code, String message) { super(message); this.code = code; }
    public static AppException of(String code, String msg){ return new AppException(code, msg); }
}
