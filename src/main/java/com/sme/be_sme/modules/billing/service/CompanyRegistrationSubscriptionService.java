package com.sme.be_sme.modules.billing.service;

import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * Creates default FREE subscription when a company registers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyRegistrationSubscriptionService {

    private static final String PLAN_CODE_FREE = "FREE";

    private final PlanMapper planMapper;
    private final SubscriptionMapper subscriptionMapper;

    /**
     * Create FREE plan subscription for the newly registered company.
     */
    public void createFreeSubscriptionForCompany(String companyId) {
        if (!StringUtils.hasText(companyId)) return;

        PlanEntity freePlan = planMapper.selectAll().stream()
                .filter(p -> p != null && PLAN_CODE_FREE.equalsIgnoreCase(p.getCode()))
                .filter(p -> p.getCompanyId() == null || p.getCompanyId().isBlank())
                .findFirst()
                .orElse(null);

        if (freePlan == null) {
            log.warn("FREE plan not found - skipping subscription creation for company {}", companyId);
            return;
        }

        Date now = new Date();
        SubscriptionEntity sub = new SubscriptionEntity();
        sub.setSubscriptionId(UuidGenerator.generate());
        sub.setCompanyId(companyId.trim());
        sub.setPlanId(freePlan.getPlanId());
        sub.setBillingCycle("MONTHLY");
        sub.setStatus("ACTIVE");
        sub.setCurrentPeriodStart(toDate(LocalDate.now()));
        sub.setCurrentPeriodEnd(toDate(LocalDate.now().plusMonths(1)));
        sub.setAutoRenew(false);
        sub.setCreatedAt(now);
        sub.setUpdatedAt(now);

        int inserted = subscriptionMapper.insert(sub);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create FREE subscription failed");
        }
        log.info("Created FREE subscription {} for company {}", sub.getSubscriptionId(), companyId);
    }

    private static Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
