package com.sme.be_sme.shared.api.error;

import com.sme.be_sme.shared.api.BaseResponse;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<BaseResponse<Object>> handleDataIntegrity(DataIntegrityViolationException ex,
                                                                   HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        String msg = ex.getMessage();
        String userMsg = "Dữ liệu không hợp lệ";
        if (msg != null) {
            if (msg.contains("uq_users_lower_email") || msg.contains("duplicate key") && msg.toLowerCase().contains("email")) {
                userMsg = "Email đã tồn tại trong hệ thống";
            } else if (msg.contains("duplicate key") || msg.contains("unique constraint")) {
                userMsg = "Dữ liệu trùng lặp";
            }
        }
        log.warn("DataIntegrityViolation [requestId={}]: {}", requestId, msg);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(BaseResponse.fail(requestId, ErrorCodes.DUPLICATED, userMsg));
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<BaseResponse<Object>> handleAppException(AppException ex, HttpServletRequest request) {
        HttpStatus status = mapStatus(ex.getCode());
        String requestId = request.getHeader("X-Request-Id");
        String message = ex.getMessage() != null ? ex.getMessage() : ex.getCode();
        return ResponseEntity.status(status)
                .body(BaseResponse.fail(requestId, ex.getCode(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Object>> handleException(Exception ex, HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        log.error("Unexpected error [requestId={}]: {}", requestId, ex.getMessage(), ex);
        String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : (ex.getClass().getSimpleName() + " - see server logs");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.fail(requestId, ErrorCodes.INTERNAL_ERROR, message));
    }

    private HttpStatus mapStatus(String code) {
        if (ErrorCodes.DUPLICATED.equals(code)
                || "COMPANY_ALREADY_EXISTS".equals(code)
                || "ADMIN_ALREADY_EXISTS".equals(code)) {
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
        if (ErrorCodes.PAYMENT_REQUIRED.equals(code)) {
            return HttpStatus.PAYMENT_REQUIRED;
        }
        if (ErrorCodes.LIMIT_EXCEEDED.equals(code)) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
