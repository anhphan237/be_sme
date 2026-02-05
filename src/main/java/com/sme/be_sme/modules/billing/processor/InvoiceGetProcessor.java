package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.InvoiceGetRequest;
import com.sme.be_sme.modules.billing.api.response.InvoiceDetailResponse;
import com.sme.be_sme.modules.billing.infrastructure.mapper.InvoiceMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.InvoiceEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class InvoiceGetProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final InvoiceMapper invoiceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        InvoiceGetRequest request = objectMapper.convertValue(payload, InvoiceGetRequest.class);
        validate(context, request);

        InvoiceEntity invoice = invoiceMapper.selectByPrimaryKey(request.getInvoiceId().trim());
        if (invoice == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "invoice not found");
        }
        if (!context.getTenantId().trim().equals(invoice.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "invoice does not belong to tenant");
        }

        InvoiceDetailResponse response = new InvoiceDetailResponse();
        response.setInvoiceId(invoice.getInvoiceId());
        response.setInvoiceNo(invoice.getInvoiceNo());
        response.setSubscriptionId(invoice.getSubscriptionId());
        response.setAmountTotal(invoice.getAmountTotal());
        response.setCurrency(invoice.getCurrency());
        response.setStatus(invoice.getStatus());
        response.setIssuedAt(invoice.getIssuedAt());
        response.setDueAt(invoice.getDueAt());
        response.setEInvoiceUrl(invoice.geteInvoiceUrl());
        return response;
    }

    private static void validate(BizContext context, InvoiceGetRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getInvoiceId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invoiceId is required");
        }
    }
}
