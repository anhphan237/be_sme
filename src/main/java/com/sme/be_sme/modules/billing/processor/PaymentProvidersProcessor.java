package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.sme.be_sme.modules.billing.api.response.PaymentProvidersResponse;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PaymentProviderMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PaymentProviderEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PaymentProvidersProcessor extends BaseBizProcessor<BizContext> {

    private static final List<String> ALL_PROVIDERS = List.of("Stripe", "MoMo", "ZaloPay", "VNPay");

    private final PaymentProviderMapper paymentProviderMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        String companyId = context.getTenantId();
        List<PaymentProviderEntity> connected = paymentProviderMapper.selectByCompanyId(companyId);
        Map<String, PaymentProviderEntity> connectedMap = connected.stream()
                .collect(Collectors.toMap(
                        e -> e.getProviderName().trim().toLowerCase(),
                        e -> e,
                        (a, b) -> a
                ));

        List<PaymentProvidersResponse.ProviderItem> items = new ArrayList<>();
        for (String name : ALL_PROVIDERS) {
            PaymentProvidersResponse.ProviderItem item = new PaymentProvidersResponse.ProviderItem();
            item.setName(name);

            PaymentProviderEntity entity = connectedMap.get(name.toLowerCase());
            if (entity != null && "CONNECTED".equalsIgnoreCase(entity.getStatus())) {
                item.setStatus("Connected");
                item.setAccountId(entity.getAccountId());
                item.setLastSync(entity.getLastSyncAt());
            } else {
                item.setStatus("Disconnected");
            }
            items.add(item);
        }

        PaymentProvidersResponse response = new PaymentProvidersResponse();
        response.setProviders(items);
        return response;
    }
}
