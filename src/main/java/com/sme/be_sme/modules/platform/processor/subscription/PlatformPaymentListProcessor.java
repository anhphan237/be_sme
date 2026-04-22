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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
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
    private static final int MAX_SIZE = 100;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ObjectMapper objectMapper;
    private final PaymentTransactionMapper paymentTransactionMapper;
    private final CompanyMapper companyMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformPaymentListRequest request = payload == null || payload.isNull()
                ? new PlatformPaymentListRequest()
                : objectMapper.convertValue(payload, PlatformPaymentListRequest.class);

        int page = request.getPage() != null && request.getPage() >= 0
                ? request.getPage()
                : DEFAULT_PAGE;

        int size = request.getSize() != null && request.getSize() > 0
                ? Math.min(request.getSize(), MAX_SIZE)
                : DEFAULT_SIZE;

        String companyId = trimToNull(request.getCompanyId());
        String subscriptionId = trimToNull(request.getSubscriptionId());
        String invoiceId = trimToNull(request.getInvoiceId());
        String status = trimToNull(request.getStatus());

        Date start = parseStartDate(request.getStartDate());
        Date end = parseEndDate(request.getEndDate());

        List<PaymentTransactionEntity> allTransactions = paymentTransactionMapper.selectAll();
        Map<String, String> companyNameMap = buildCompanyNameMap();

        List<PaymentTransactionEntity> filtered = new ArrayList<>();

        for (PaymentTransactionEntity txn : allTransactions) {
            if (txn == null) {
                continue;
            }

            if (StringUtils.hasText(companyId)
                    && !companyId.equals(txn.getCompanyId())) {
                continue;
            }

            if (StringUtils.hasText(subscriptionId)
                    && !subscriptionId.equals(txn.getSubscriptionId())) {
                continue;
            }

            if (StringUtils.hasText(invoiceId)
                    && !invoiceId.equals(txn.getInvoiceId())) {
                continue;
            }

            if (StringUtils.hasText(status)
                    && !status.equalsIgnoreCase(txn.getStatus())) {
                continue;
            }

            if (start != null && txn.getCreatedAt() != null && txn.getCreatedAt().before(start)) {
                continue;
            }

            if (end != null && txn.getCreatedAt() != null && !txn.getCreatedAt().before(end)) {
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

            item.setSubscriptionId(txn.getSubscriptionId());
            item.setInvoiceId(txn.getInvoiceId());

            item.setProvider(txn.getProvider());
            item.setProviderTxnId(txn.getProviderTxnId());

            item.setAmount(txn.getAmount());
            item.setCurrency(txn.getCurrency());
            item.setStatus(txn.getStatus());
            item.setFailureReason(txn.getFailureReason());

            item.setCreatedAt(txn.getCreatedAt());
            item.setPaidAt(txn.getPaidAt());

            items.add(item);
        }

        PlatformPaymentListResponse response = new PlatformPaymentListResponse();
        response.setItems(items);
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);

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

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static Date parseStartDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        LocalDate date = LocalDate.parse(value.trim(), DATE_FORMATTER);
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static Date parseEndDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        LocalDate date = LocalDate.parse(value.trim(), DATE_FORMATTER);
        return Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}