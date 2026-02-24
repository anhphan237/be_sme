package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
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

    private final PaymentTransactionMapperExt paymentTransactionMapperExt;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        String companyId = context.getTenantId();
        List<PaymentTransactionEntity> entities = paymentTransactionMapperExt.selectByCompanyId(companyId);

        List<PaymentTransactionsResponse.TransactionItem> items = entities.stream()
                .map(e -> {
                    PaymentTransactionsResponse.TransactionItem item = new PaymentTransactionsResponse.TransactionItem();
                    item.setId(e.getPaymentTransactionId());
                    item.setInvoiceId(e.getInvoiceId());
                    item.setAmount(e.getAmount());
                    item.setCurrency(e.getCurrency());
                    item.setStatus(e.getStatus());
                    item.setProvider(e.getProvider());
                    item.setCreatedAt(e.getCreatedAt());
                    item.setCompanyId(e.getCompanyId());
                    return item;
                })
                .collect(Collectors.toList());

        PaymentTransactionsResponse response = new PaymentTransactionsResponse();
        response.setTransactions(items);
        return response;
    }
}
