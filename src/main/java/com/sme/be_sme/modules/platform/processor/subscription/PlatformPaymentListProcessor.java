package com.sme.be_sme.modules.platform.processor.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PaymentTransactionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformPaymentListRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformPaymentListResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformPaymentListResponse.PaymentItem;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PlatformPaymentListProcessor extends BaseBizProcessor<BizContext> {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final ObjectMapper objectMapper;
    private final PaymentTransactionMapper paymentTransactionMapper;
    private final CompanyMapper companyMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformPaymentListRequest request = objectMapper.convertValue(payload, PlatformPaymentListRequest.class);

        int page = request.getPage() != null ? request.getPage() : DEFAULT_PAGE;
        int size = request.getSize() != null ? request.getSize() : DEFAULT_SIZE;

        List<PaymentTransactionEntity> allTransactions = paymentTransactionMapper.selectAll();
        Map<String, String> companyNameMap = buildCompanyNameMap();

        List<PaymentTransactionEntity> filtered = new ArrayList<>();
        for (PaymentTransactionEntity txn : allTransactions) {
            if (txn == null) continue;

            if (StringUtils.hasText(request.getStatus())
                    && !request.getStatus().equalsIgnoreCase(txn.getStatus())) {
                continue;
            }
            filtered.add(txn);
        }

        int total = filtered.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<PaymentTransactionEntity> pageSlice = filtered.subList(fromIndex, toIndex);

        List<PaymentItem> items = new ArrayList<>();
        for (PaymentTransactionEntity txn : pageSlice) {
            PaymentItem item = new PaymentItem();
            item.setTransactionId(txn.getPaymentTransactionId());
            item.setCompanyId(txn.getCompanyId());
            item.setCompanyName(companyNameMap.get(txn.getCompanyId()));
            item.setAmount(txn.getAmount());
            item.setCurrency(txn.getCurrency());
            item.setStatus(txn.getStatus());
            item.setCreatedAt(txn.getCreatedAt());
            items.add(item);
        }

        PlatformPaymentListResponse response = new PlatformPaymentListResponse();
        response.setItems(items);
        response.setTotal(total);
        return response;
    }

    private Map<String, String> buildCompanyNameMap() {
        Map<String, String> map = new HashMap<>();
        for (CompanyEntity company : companyMapper.selectAll()) {
            if (company != null && company.getCompanyId() != null) {
                map.put(company.getCompanyId(), company.getName());
            }
        }
        return map;
    }
}
