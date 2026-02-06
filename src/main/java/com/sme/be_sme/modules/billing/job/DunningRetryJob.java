package com.sme.be_sme.modules.billing.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sme.be_sme.modules.billing.api.response.DunningRetryResponse;
import com.sme.be_sme.modules.billing.infrastructure.mapper.DunningCaseMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.DunningCaseMapperExt;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.DunningCaseEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.billing.processor.DunningRetryProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.gateway.core.BizContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * Internal scheduled job: scans dunning_cases with next_retry_at <= now and status = PENDING_RETRY,
 * triggers retry (create payment intent). After MAX_RETRIES failures updates case to SUSPENDED
 * and subscription status to SUSPENDED. Optional: send "payment failed" / "account suspended"
 * email or notification (can be added via NotificationMapper or email service).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DunningRetryJob {

    private static final int MAX_RETRIES_BEFORE_SUSPEND = 5;
    private static final String STATUS_SUSPENDED = "SUSPENDED";

    private final DunningCaseMapperExt dunningCaseMapperExt;
    private final DunningCaseMapper dunningCaseMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final DunningRetryProcessor dunningRetryProcessor;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "${app.dunning.retry.cron:0 0 9 * * ?}") // default: daily at 09:00
    public void run() {
        Date now = new Date();
        List<DunningCaseEntity> due = dunningCaseMapperExt.selectPendingRetryDue(now);
        if (due == null || due.isEmpty()) {
            return;
        }
        log.info("DunningRetryJob: processing {} cases due for retry", due.size());
        for (DunningCaseEntity caseEntity : due) {
            try {
                runRetryForCase(caseEntity);
            } catch (Exception e) {
                log.warn("DunningRetryJob: retry failed for case {}: {}", caseEntity.getDunningCaseId(), e.getMessage());
            }
        }
    }

    private void runRetryForCase(DunningCaseEntity caseEntity) {
        BizContext ctx = new BizContext();
        ctx.setTenantId(caseEntity.getCompanyId());
        ctx.setRequestId("dunning-retry-job");
        ctx.setOperationType("com.sme.billing.dunning.retry");
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("dunningCaseId", caseEntity.getDunningCaseId());
        ctx.setPayload(payload);

        BizContextHolder.set(ctx);
        try {
            Object result = dunningRetryProcessor.execute(ctx);
            if (result instanceof DunningRetryResponse) {
                DunningRetryResponse resp = (DunningRetryResponse) result;
                if (!resp.isSuccess() && resp.getRetryCount() != null && resp.getRetryCount() >= MAX_RETRIES_BEFORE_SUSPEND) {
                    suspendCaseAndSubscription(caseEntity);
                }
            }
        } finally {
            BizContextHolder.clear();
        }
    }

    private void suspendCaseAndSubscription(DunningCaseEntity caseEntity) {
        Date now = new Date();
        caseEntity.setStatus(STATUS_SUSPENDED);
        caseEntity.setUpdatedAt(now);
        dunningCaseMapper.updateByPrimaryKey(caseEntity);
        log.info("DunningRetryJob: suspended dunning case {} after {} retries", caseEntity.getDunningCaseId(), caseEntity.getRetryCount());

        if (caseEntity.getSubscriptionId() != null) {
            SubscriptionEntity sub = subscriptionMapper.selectByPrimaryKey(caseEntity.getSubscriptionId());
            if (sub != null && !STATUS_SUSPENDED.equalsIgnoreCase(sub.getStatus())) {
                sub.setStatus(STATUS_SUSPENDED);
                sub.setUpdatedAt(now);
                subscriptionMapper.updateByPrimaryKey(sub);
                log.info("DunningRetryJob: suspended subscription {}", sub.getSubscriptionId());
            }
        }
        // Optional: send "account suspended" notification or email (e.g. to company admin)
    }
}
