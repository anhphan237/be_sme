package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.PaymentStatusRequest;
import com.sme.be_sme.modules.billing.api.response.PaymentStatusResponse;
import com.sme.be_sme.modules.billing.infrastructure.gateway.PaymentGatewayPort;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PaymentTransactionMapperExt;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentStatusProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final PaymentTransactionMapperExt paymentTransactionMapperExt;

    @Value("${app.stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${app.payment.gateway:mock}")
    private String paymentGateway;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PaymentStatusRequest request = objectMapper.convertValue(payload, PaymentStatusRequest.class);
        validate(context, request);

        String companyId = context.getTenantId();
        String paymentIntentId = request.getPaymentIntentId().trim();

        PaymentTransactionEntity txn = paymentTransactionMapperExt.selectByProviderTxnId(paymentIntentId);
        if (txn == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "payment transaction not found");
        }
        if (!companyId.equals(txn.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "transaction does not belong to tenant");
        }

        String liveStatus = fetchLiveStatus(paymentIntentId);

        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setId(txn.getProviderTxnId());
        response.setAmount(txn.getAmount());
        response.setCurrency(txn.getCurrency());
        response.setStatus(liveStatus != null ? liveStatus : txn.getStatus());
        response.setInvoiceId(txn.getInvoiceId());
        return response;
    }

    private String fetchLiveStatus(String paymentIntentId) {
        if (!"stripe".equalsIgnoreCase(paymentGateway) || !StringUtils.hasText(stripeSecretKey)) {
            return null;
        }
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(stripeSecretKey.trim());
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(
                    "https://api.stripe.com/v1/payment_intents/" + paymentIntentId,
                    HttpMethod.GET, entity, String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonNode root = objectMapper.readTree(resp.getBody());
                return root.has("status") ? root.path("status").asText() : null;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch Stripe payment intent status: {}", e.getMessage());
        }
        return null;
    }

    private static void validate(BizContext context, PaymentStatusRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getPaymentIntentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "paymentIntentId is required");
        }
    }
}
