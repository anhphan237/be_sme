package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.PaymentCreateIntentRequest;
import com.sme.be_sme.modules.billing.enums.InvoiceStatus;
import com.sme.be_sme.modules.billing.enums.PaymentTransactionStatus;
import com.sme.be_sme.modules.billing.api.response.PaymentCreateIntentResponse;
import com.sme.be_sme.modules.billing.infrastructure.gateway.PaymentGatewayPort;
import com.sme.be_sme.modules.billing.infrastructure.mapper.InvoiceMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PaymentTransactionMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PaymentTransactionMapperExt;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.InvoiceEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class PaymentCreateIntentProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final InvoiceMapper invoiceMapper;
    private final PaymentTransactionMapper paymentTransactionMapper;
    private final PaymentTransactionMapperExt paymentTransactionMapperExt;
    private final PaymentGatewayPort paymentGateway;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PaymentCreateIntentRequest request = objectMapper.convertValue(payload, PaymentCreateIntentRequest.class);
        validate(context, request);

        String companyId = context.getTenantId();
        String invoiceId = request.getInvoiceId().trim();

        InvoiceEntity invoice = invoiceMapper.selectByPrimaryKey(invoiceId);
        if (invoice == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "invoice not found");
        }
        if (!companyId.equals(invoice.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "invoice does not belong to tenant");
        }
        if (!InvoiceStatus.ISSUED.getCode().equalsIgnoreCase(invoice.getStatus())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invoice is not payable");
        }

        Integer amount = invoice.getAmountTotal() != null ? invoice.getAmountTotal() : 0;
        if (amount <= 0) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invoice amount must be greater than 0");
        }
        String currency = StringUtils.hasText(invoice.getCurrency()) ? invoice.getCurrency() : "VND";

        PaymentGatewayPort.CreateIntentResult result = paymentGateway.createIntent(companyId, invoiceId, amount, currency);

        PaymentTransactionEntity existingForPi = paymentTransactionMapperExt.selectByProviderTxnId(result.getPaymentIntentId());
        if (existingForPi != null
                && companyId.equals(existingForPi.getCompanyId())
                && invoiceId.equals(existingForPi.getInvoiceId())) {
            return buildResponse(
                    existingForPi.getPaymentTransactionId(),
                    invoiceId,
                    result.getPaymentIntentId(),
                    result.getClientSecret(),
                    result.getGatewayName(),
                    amount,
                    currency,
                    result.getStatus());
        }

        String txnId = UuidGenerator.generate();
        Date now = new Date();
        PaymentTransactionEntity txn = new PaymentTransactionEntity();
        txn.setPaymentTransactionId(txnId);
        txn.setCompanyId(companyId);
        txn.setSubscriptionId(invoice.getSubscriptionId());
        txn.setInvoiceId(invoiceId);
        txn.setProvider(result.getGatewayName());
        txn.setProviderTxnId(result.getPaymentIntentId());
        txn.setAmount(amount);
        txn.setCurrency(currency);
        PaymentTransactionStatus txnStatus = PaymentTransactionStatus.fromCode(result.getStatus());
        txn.setStatus(txnStatus != null ? txnStatus.getCode() : PaymentTransactionStatus.PENDING.getCode());
        txn.setCreatedAt(now);
        paymentTransactionMapper.insert(txn);

        return buildResponse(
                txnId,
                invoiceId,
                result.getPaymentIntentId(),
                result.getClientSecret(),
                result.getGatewayName(),
                amount,
                currency,
                result.getStatus());
    }

    private static PaymentCreateIntentResponse buildResponse(String paymentTransactionId,
                                                             String invoiceId,
                                                             String paymentIntentId,
                                                             String clientSecret,
                                                             String gatewayName,
                                                             Integer amount,
                                                             String currency,
                                                             String status) {
        PaymentCreateIntentResponse response = new PaymentCreateIntentResponse();
        response.setPaymentTransactionId(paymentTransactionId);
        response.setPaymentIntentId(paymentIntentId);
        response.setClientSecret(clientSecret);
        response.setGateway(gatewayName);
        response.setAmount(amount);
        response.setCurrency(currency);
        response.setStatus(status);
        response.setInvoiceId(invoiceId);
        return response;
    }

    private static void validate(BizContext context, PaymentCreateIntentRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getInvoiceId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invoiceId is required");
        }
    }
}
