package com.sme.be_sme.shared.api.error;

import com.sme.be_sme.shared.api.BaseResponse;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<BaseResponse<Object>> handleAppException(AppException ex, HttpServletRequest request) {
        HttpStatus status = mapStatus(ex.getCode());
        String requestId = request.getHeader("X-Request-Id");
        return ResponseEntity.status(status)
                .body(BaseResponse.fail(requestId, ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Object>> handleException(Exception ex, HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.fail(requestId, ErrorCodes.INTERNAL_ERROR, ex.getMessage()));
    }

    private HttpStatus mapStatus(String code) {
        if ("COMPANY_ALREADY_EXISTS".equals(code) || "ADMIN_ALREADY_EXISTS".equals(code)) {
            return HttpStatus.CONFLICT;
        }
        if (ErrorCodes.BAD_REQUEST.equals(code)) {
            return HttpStatus.BAD_REQUEST;
        }
        if (ErrorCodes.UNAUTHORIZED.equals(code)) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (ErrorCodes.FORBIDDEN.equals(code)) {
            return HttpStatus.FORBIDDEN;
        }
        if (ErrorCodes.NOT_FOUND.equals(code)) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
