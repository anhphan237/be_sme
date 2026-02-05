package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.InvoiceListRequest;
import com.sme.be_sme.modules.billing.api.response.InvoiceListResponse;
import com.sme.be_sme.modules.billing.api.response.InvoiceSummaryResponse;
import com.sme.be_sme.modules.billing.infrastructure.mapper.InvoiceMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.InvoiceEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class InvoiceListProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final InvoiceMapper invoiceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        InvoiceListRequest request = objectMapper.convertValue(payload, InvoiceListRequest.class);
        validate(context);

        String companyId = context.getTenantId().trim();
        String subscriptionId = request == null ? null : request.getSubscriptionId();
        String status = request == null ? null : request.getStatus();
        String statusNormalized = status == null ? null : status.trim().toLowerCase(Locale.ROOT);

        List<InvoiceSummaryResponse> invoices = invoiceMapper.selectAll().stream()
                .filter(Objects::nonNull)
                .filter(row -> companyId.equals(row.getCompanyId()))
                .filter(row -> !StringUtils.hasText(subscriptionId)
                        || subscriptionId.trim().equals(row.getSubscriptionId()))
                .filter(row -> !StringUtils.hasText(statusNormalized)
                        || (row.getStatus() != null && row.getStatus().trim().toLowerCase(Locale.ROOT).equals(statusNormalized)))
                .map(this::toSummary)
                .collect(Collectors.toList());

        InvoiceListResponse response = new InvoiceListResponse();
        response.setInvoices(invoices);
        return response;
    }

    private static void validate(BizContext context) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
    }

    private InvoiceSummaryResponse toSummary(InvoiceEntity entity) {
        InvoiceSummaryResponse response = new InvoiceSummaryResponse();
        response.setInvoiceId(entity.getInvoiceId());
        response.setInvoiceNo(entity.getInvoiceNo());
        response.setAmountTotal(entity.getAmountTotal());
        response.setCurrency(entity.getCurrency());
        response.setStatus(entity.getStatus());
        response.setIssuedAt(entity.getIssuedAt());
        response.setDueAt(entity.getDueAt());
        return response;
    }
}
