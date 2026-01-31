package com.sme.be_sme.shared.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> {
    private String code;
    private String message;
    private String requestId;
    private T data;

    public static <T> BaseResponse<T> success(String requestId, T data) {
        return new BaseResponse<>("SUCCESS", "OK", requestId, data);
    }

    public static <T> BaseResponse<T> fail(String requestId, String code, String message) {
        return new BaseResponse<>(code, message, requestId, null);
    }
}
