package com.sme.be_sme.modules.platform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.platform.infrastructure.mapper.ErrorLogMapper;
import com.sme.be_sme.modules.platform.infrastructure.persistence.entity.ErrorLogEntity;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PlatformErrorLogSupport {

    private final ErrorLogMapper errorLogMapper;
    private final ObjectMapper objectMapper;

    public void log(
            BizContext context,
            String requestId,
            String operationType,
            JsonNode payload,
            Exception ex
    ) {
        try {
            ErrorLogEntity entity = new ErrorLogEntity();

            entity.setErrorId(UUID.randomUUID().toString());
            entity.setRequestId(requestId);
            entity.setOperationType(operationType);

            entity.setErrorCode(resolveErrorCode(ex));
            entity.setMessage(resolveMessage(ex));
            entity.setStackTrace(toStackTrace(ex));
            entity.setSeverity(resolveSeverity(ex));
            entity.setStatus("OPEN");

            if (context != null) {
                entity.setTenantId(context.getTenantId());
                entity.setActorUserId(context.getOperatorId());
                entity.setActorRole(context.getRoles() == null ? null : String.join(",", context.getRoles()));
            }

            entity.setCompanyId(resolveCompanyId(context, payload));
            entity.setPayloadSnapshot(payload == null ? null : objectMapper.writeValueAsString(payload));

            errorLogMapper.insert(entity);
        } catch (Exception ignored) {
            // Không được để lỗi ghi log làm hỏng API chính.
        }
    }

    private String resolveErrorCode(Exception ex) {
        String code = tryInvokeStringGetter(ex, "getCode");
        if (StringUtils.hasText(code)) {
            return code;
        }

        code = tryInvokeStringGetter(ex, "getErrorCode");
        if (StringUtils.hasText(code)) {
            return code;
        }

        String className = ex.getClass().getSimpleName().toUpperCase(Locale.ROOT);
        String message = ex.getMessage() == null ? "" : ex.getMessage().toUpperCase(Locale.ROOT);

        if (className.contains("PSQL") || className.contains("SQL") || message.contains("SQL")) {
            return "DB_ERROR";
        }

        if (message.contains("UNSUPPORTED OPERATIONTYPE")) {
            return "UNSUPPORTED_OPERATION";
        }

        if (message.contains("FORBIDDEN") || message.contains("ACCESS DENIED")) {
            return "FORBIDDEN";
        }

        if (message.contains("UNAUTHORIZED") || message.contains("TOKEN")) {
            return "UNAUTHORIZED";
        }

        if (message.contains("BAD_REQUEST")
                || message.contains("REQUIRED")
                || message.contains("DOES NOT MATCH")
                || message.contains("INVALID")) {
            return "BAD_REQUEST";
        }

        return "INTERNAL_ERROR";
    }

    private String resolveSeverity(Exception ex) {
        String code = resolveErrorCode(ex);

        if ("DB_ERROR".equals(code) || "INTERNAL_ERROR".equals(code)) {
            return "HIGH";
        }

        if ("UNSUPPORTED_OPERATION".equals(code)) {
            return "MEDIUM";
        }

        if ("FORBIDDEN".equals(code) || "UNAUTHORIZED".equals(code)) {
            return "MEDIUM";
        }

        if ("BAD_REQUEST".equals(code)) {
            return "LOW";
        }

        return "MEDIUM";
    }

    private String resolveMessage(Exception ex) {
        if (StringUtils.hasText(ex.getMessage())) {
            return ex.getMessage();
        }
        return ex.getClass().getSimpleName();
    }

    private String resolveCompanyId(BizContext context, JsonNode payload) {
        if (payload != null && payload.hasNonNull("companyId")) {
            return payload.get("companyId").asText();
        }

        if (context != null && StringUtils.hasText(context.getTenantId())) {
            return context.getTenantId();
        }

        return null;
    }

    private String toStackTrace(Exception ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private String tryInvokeStringGetter(Exception ex, String methodName) {
        try {
            Method method = ex.getClass().getMethod(methodName);
            Object value = method.invoke(ex);
            return value == null ? null : value.toString();
        } catch (Exception ignored) {
            return null;
        }
    }
}