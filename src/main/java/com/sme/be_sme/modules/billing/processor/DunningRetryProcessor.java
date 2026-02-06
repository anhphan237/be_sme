package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.DunningRetryRequest;
import com.sme.be_sme.modules.billing.api.response.DunningRetryResponse;
import com.sme.be_sme.modules.billing.infrastructure.gateway.PaymentGatewayPort;
import com.sme.be_sme.modules.billing.infrastructure.mapper.DunningCaseMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.DunningCaseMapperExt;
import com.sme.be_sme.modules.billing.infrastructure.mapper.InvoiceMapperExt;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PaymentTransactionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.DunningCaseEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.InvoiceEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.sme.be_sme.modules.notification.infrastructure.mapper.NotificationMapper;
import com.sme.be_sme.modules.notification.infrastructure.persistence.entity.NotificationEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Calendar;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class DunningRetryProcessor extends BaseBizProcessor<BizContext> {

    private static final String STATUS_PENDING_RETRY = "PENDING_RETRY";
    private static final int NEXT_RETRY_DAYS_SUCCESS = 3;
    private static final int NEXT_RETRY_DAYS_FAIL = 1;
    private static final String NOTIFICATION_TYPE_PAYMENT_FAILED = "PAYMENT_FAILED";

    private final ObjectMapper objectMapper;
    private final DunningCaseMapper dunningCaseMapper;
    private final DunningCaseMapperExt dunningCaseMapperExt;
    private final InvoiceMapperExt invoiceMapperExt;
    private final PaymentGatewayPort paymentGateway;
    private final PaymentTransactionMapper paymentTransactionMapper;
    private final NotificationMapper notificationMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DunningRetryRequest request = objectMapper.convertValue(payload, DunningRetryRequest.class);
        validate(context, request);

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();

        DunningCaseEntity dunningCase = resolveDunningCase(companyId, request);
        if (dunningCase == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "dunning case not found");
        }
        if (!companyId.equals(dunningCase.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "dunning case does not belong to tenant");
        }

        InvoiceEntity invoice = invoiceMapperExt.selectLatestUnpaidBySubscriptionId(companyId, dunningCase.getSubscriptionId());
        if (invoice == null) {
            updateDunningAfterFailure(dunningCase);
            notifyPaymentFailed(companyId, operatorId, dunningCase.getDunningCaseId(), "No unpaid invoice for subscription");
            return failureResponse(dunningCase, "No unpaid invoice found for this subscription");
        }

        try {
            Integer amount = invoice.getAmountTotal() != null ? invoice.getAmountTotal() : 0;
            String currency = StringUtils.hasText(invoice.getCurrency()) ? invoice.getCurrency() : "VND";
            PaymentGatewayPort.CreateIntentResult result = paymentGateway.createIntent(companyId, invoice.getInvoiceId(), amount, currency);

            String txnId = UuidGenerator.generate();
            Date now = new Date();
            PaymentTransactionEntity txn = new PaymentTransactionEntity();
            txn.setPaymentTransactionId(txnId);
            txn.setCompanyId(companyId);
            txn.setSubscriptionId(dunningCase.getSubscriptionId());
            txn.setInvoiceId(invoice.getInvoiceId());
            txn.setProvider(result.getGatewayName());
            txn.setProviderTxnId(result.getPaymentIntentId());
            txn.setAmount(amount);
            txn.setStatus(result.getStatus());
            txn.setCreatedAt(now);
            paymentTransactionMapper.insert(txn);

            int newRetryCount = (dunningCase.getRetryCount() != null ? dunningCase.getRetryCount() : 0) + 1;
            dunningCase.setRetryCount(newRetryCount);
            dunningCase.setNextRetryAt(addDays(now, NEXT_RETRY_DAYS_SUCCESS));
            dunningCase.setStatus(STATUS_PENDING_RETRY);
            dunningCase.setUpdatedAt(now);
            dunningCaseMapper.updateByPrimaryKey(dunningCase);

            DunningRetryResponse response = new DunningRetryResponse();
            response.setSuccess(true);
            response.setMessage("Payment intent created; complete payment on the gateway.");
            response.setPaymentIntentId(result.getPaymentIntentId());
            response.setClientSecret(result.getClientSecret());
            response.setGateway(result.getGatewayName());
            response.setRetryCount(newRetryCount);
            return response;
        } catch (Exception e) {
            updateDunningAfterFailure(dunningCase);
            notifyPaymentFailed(companyId, operatorId, dunningCase.getDunningCaseId(), e.getMessage());
            return failureResponse(dunningCase, "Retry failed: " + (e.getMessage() != null ? e.getMessage() : "unknown error"));
        }
    }

    private DunningCaseEntity resolveDunningCase(String companyId, DunningRetryRequest request) {
        if (StringUtils.hasText(request.getDunningCaseId())) {
            DunningCaseEntity c = dunningCaseMapper.selectByPrimaryKey(request.getDunningCaseId().trim());
            return (c != null && companyId.equals(c.getCompanyId())) ? c : null;
        }
        if (StringUtils.hasText(request.getSubscriptionId())) {
            return dunningCaseMapperExt.selectByCompanyIdAndSubscriptionId(companyId, request.getSubscriptionId().trim());
        }
        return null;
    }

    private void updateDunningAfterFailure(DunningCaseEntity dunningCase) {
        int newRetryCount = (dunningCase.getRetryCount() != null ? dunningCase.getRetryCount() : 0) + 1;
        Date now = new Date();
        dunningCase.setRetryCount(newRetryCount);
        dunningCase.setNextRetryAt(addDays(now, NEXT_RETRY_DAYS_FAIL));
        dunningCase.setStatus(STATUS_PENDING_RETRY);
        dunningCase.setUpdatedAt(now);
        dunningCaseMapper.updateByPrimaryKey(dunningCase);
    }

    private void notifyPaymentFailed(String companyId, String userId, String refId, String reason) {
        if (!StringUtils.hasText(userId)) return;
        Date now = new Date();
        NotificationEntity n = new NotificationEntity();
        n.setNotificationId(UuidGenerator.generate());
        n.setCompanyId(companyId);
        n.setUserId(userId);
        n.setType(NOTIFICATION_TYPE_PAYMENT_FAILED);
        n.setTitle("Payment retry failed");
        n.setContent("Dunning retry failed" + (StringUtils.hasText(reason) ? ": " + reason : "."));
        n.setStatus("UNREAD");
        n.setRefType("DUNNING");
        n.setRefId(refId);
        n.setCreatedAt(now);
        notificationMapper.insert(n);
    }

    private DunningRetryResponse failureResponse(DunningCaseEntity dunningCase, String message) {
        int retryCount = dunningCase.getRetryCount() != null ? dunningCase.getRetryCount() : 0;
        DunningRetryResponse response = new DunningRetryResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setRetryCount(retryCount);
        return response;
    }

    private static Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days);
        return cal.getTime();
    }

    private static void validate(BizContext context, DunningRetryRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getDunningCaseId()) && !StringUtils.hasText(request.getSubscriptionId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "dunningCaseId or subscriptionId is required");
        }
    }
}
