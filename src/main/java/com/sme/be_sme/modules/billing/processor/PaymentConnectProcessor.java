package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.PaymentConnectRequest;
import com.sme.be_sme.modules.billing.api.response.PaymentConnectResponse;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PaymentProviderMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PaymentProviderEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PaymentConnectProcessor extends BaseBizProcessor<BizContext> {

    private static final Set<String> SUPPORTED_PROVIDERS = Set.of("stripe", "momo", "zalopay", "vnpay");

    private final ObjectMapper objectMapper;
    private final PaymentProviderMapper paymentProviderMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PaymentConnectRequest request = objectMapper.convertValue(payload, PaymentConnectRequest.class);
        validate(context, request);

        String companyId = context.getTenantId();
        String providerName = request.getProvider().trim();
        Date now = new Date();

        PaymentProviderEntity existing = paymentProviderMapper.selectByCompanyIdAndProviderName(companyId, providerName);
        if (existing != null) {
            existing.setStatus("CONNECTED");
            existing.setUpdatedAt(now);
            existing.setLastSyncAt(now);
            paymentProviderMapper.updateByPrimaryKey(existing);
        } else {
            PaymentProviderEntity entity = new PaymentProviderEntity();
            entity.setPaymentProviderId(UuidGenerator.generate());
            entity.setCompanyId(companyId);
            entity.setProviderName(providerName);
            entity.setStatus("CONNECTED");
            entity.setLastSyncAt(now);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            paymentProviderMapper.insert(entity);
        }

        PaymentConnectResponse response = new PaymentConnectResponse();
        response.setOk(true);
        return response;
    }

    private static void validate(BizContext context, PaymentConnectRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getProvider())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "provider is required");
        }
        if (!SUPPORTED_PROVIDERS.contains(request.getProvider().trim().toLowerCase())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "unsupported provider: " + request.getProvider());
        }
    }
}
