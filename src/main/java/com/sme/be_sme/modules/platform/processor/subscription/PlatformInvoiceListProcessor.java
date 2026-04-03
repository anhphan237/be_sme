package com.sme.be_sme.modules.platform.processor.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.InvoiceMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.InvoiceEntity;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformInvoiceListRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformInvoiceListResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformInvoiceListResponse.InvoiceItem;
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
public class PlatformInvoiceListProcessor extends BaseBizProcessor<BizContext> {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final ObjectMapper objectMapper;
    private final InvoiceMapper invoiceMapper;
    private final CompanyMapper companyMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformInvoiceListRequest request = objectMapper.convertValue(payload, PlatformInvoiceListRequest.class);

        int page = request.getPage() != null ? request.getPage() : DEFAULT_PAGE;
        int size = request.getSize() != null ? request.getSize() : DEFAULT_SIZE;

        List<InvoiceEntity> allInvoices = invoiceMapper.selectAll();
        Map<String, String> companyNameMap = buildCompanyNameMap();

        List<InvoiceEntity> filtered = new ArrayList<>();
        for (InvoiceEntity invoice : allInvoices) {
            if (invoice == null) continue;

            if (StringUtils.hasText(request.getStatus())
                    && !request.getStatus().equalsIgnoreCase(invoice.getStatus())) {
                continue;
            }
            filtered.add(invoice);
        }

        int total = filtered.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<InvoiceEntity> pageSlice = filtered.subList(fromIndex, toIndex);

        List<InvoiceItem> items = new ArrayList<>();
        for (InvoiceEntity invoice : pageSlice) {
            InvoiceItem item = new InvoiceItem();
            item.setInvoiceId(invoice.getInvoiceId());
            item.setInvoiceNo(invoice.getInvoiceNo());
            item.setCompanyId(invoice.getCompanyId());
            item.setCompanyName(companyNameMap.get(invoice.getCompanyId()));
            item.setAmountTotal(invoice.getAmountTotal());
            item.setCurrency(invoice.getCurrency());
            item.setStatus(invoice.getStatus());
            item.setDueAt(invoice.getDueAt());
            items.add(item);
        }

        PlatformInvoiceListResponse response = new PlatformInvoiceListResponse();
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
