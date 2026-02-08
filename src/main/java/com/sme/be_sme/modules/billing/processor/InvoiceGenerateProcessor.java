package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.InvoiceGenerateRequest;
import com.sme.be_sme.modules.billing.api.response.InvoiceGenerateResponse;
import com.sme.be_sme.modules.billing.infrastructure.gateway.EInvoicePort;
import com.sme.be_sme.modules.billing.infrastructure.mapper.InvoiceMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.InvoiceEntity;
import com.sme.be_sme.modules.billing.service.InvoiceGenerateCoreService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
@RequiredArgsConstructor
public class InvoiceGenerateProcessor extends BaseBizProcessor<BizContext> {

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ObjectMapper objectMapper;
    private final InvoiceGenerateCoreService invoiceGenerateCoreService;
    private final EInvoicePort eInvoicePort;
    private final InvoiceMapper invoiceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        InvoiceGenerateRequest request = objectMapper.convertValue(payload, InvoiceGenerateRequest.class);
        validate(context, request);

        LocalDate periodStart = parseDate(request.getPeriodStart(), "periodStart");
        LocalDate periodEnd = parseDate(request.getPeriodEnd(), "periodEnd");

        InvoiceEntity invoice = invoiceGenerateCoreService.generateForPeriod(
                context.getTenantId().trim(),
                request.getSubscriptionId().trim(),
                periodStart,
                periodEnd
        );

        if (eInvoicePort != null) {
            EInvoicePort.SubmitResult eInvoiceResult = eInvoicePort.submit(
                    invoice.getCompanyId(),
                    invoice.getInvoiceId(),
                    invoice.getInvoiceNo(),
                    invoice.getAmountTotal(),
                    invoice.getCurrency()
            );
            if (eInvoiceResult.isSuccess() && eInvoiceResult.getEInvoiceUrl() != null) {
                invoice.seteInvoiceUrl(eInvoiceResult.getEInvoiceUrl());
                invoiceMapper.updateByPrimaryKey(invoice);
            }
        }

        InvoiceGenerateResponse response = new InvoiceGenerateResponse();
        response.setInvoiceId(invoice.getInvoiceId());
        response.setStatus(invoice.getStatus());
        return response;
    }

    private static void validate(BizContext context, InvoiceGenerateRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getSubscriptionId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "subscriptionId is required");
        }
        if (!StringUtils.hasText(request.getPeriodStart())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "periodStart is required");
        }
        if (!StringUtils.hasText(request.getPeriodEnd())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "periodEnd is required");
        }
    }

    private static LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value.trim(), PERIOD_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, fieldName + " must be ISO-8601 yyyy-MM-dd");
        }
    }
}
