package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.PaymentTransactionsRequest;
import com.sme.be_sme.modules.billing.api.response.PaymentTransactionsResponse;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PaymentTransactionMapperExt;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PaymentTransactionsProcessor extends BaseBizProcessor<BizContext> {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ObjectMapper objectMapper;
    private final PaymentTransactionMapperExt paymentTransactionMapperExt;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        PaymentTransactionsRequest request = objectMapper.convertValue(payload, PaymentTransactionsRequest.class);
        int page = resolvePage(request == null ? null : request.getPage());
        int pageSize = resolvePageSize(request == null ? null : request.getPageSize());
        int offset = (page - 1) * pageSize;

        String companyId = context.getTenantId();
        long total = paymentTransactionMapperExt.countByCompanyId(companyId);
        List<PaymentTransactionEntity> entities = paymentTransactionMapperExt.selectByCompanyIdPaged(companyId, pageSize, offset);

        List<PaymentTransactionsResponse.TransactionItem> items = entities.stream()
                .map(e -> {
                    PaymentTransactionsResponse.TransactionItem item = new PaymentTransactionsResponse.TransactionItem();
                    item.setId(e.getPaymentTransactionId());
                    item.setInvoiceId(e.getInvoiceId());
                    item.setAmount(e.getAmount());
                    item.setAmountInDecimal(e.getAmount() == null ? null : e.getAmount().doubleValue());
                    item.setCurrency(StringUtils.hasText(e.getCurrency()) ? e.getCurrency() : "VND");
                    item.setStatus(e.getStatus());
                    item.setType("CHARGE");
                    item.setPaymentMethod("UNKNOWN");
                    item.setProvider(e.getProvider());
                    item.setProviderTransactionId(e.getProviderTxnId());
                    item.setDescription(buildDescription(e));
                    item.setCreatedAt(e.getCreatedAt());
                    item.setCompanyId(e.getCompanyId());
                    return item;
                })
                .collect(Collectors.toList());

        PaymentTransactionsResponse response = new PaymentTransactionsResponse();
        response.setTransactions(items);
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setTotal(total);
        return response;
    }

    private static int resolvePage(Integer page) {
        if (page == null || page < 1) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private static int resolvePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private static String buildDescription(PaymentTransactionEntity entity) {
        if (entity == null) {
            return "Subscription payment";
        }
        if (StringUtils.hasText(entity.getInvoiceId())) {
            return "Subscription payment for invoice " + entity.getInvoiceId();
        }
        return "Subscription payment";
    }
}
