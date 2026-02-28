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
 * Creates subscription for company registration using the plan provided by FE.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyRegistrationSubscriptionService {

    private final PlanMapper planMapper;
    private final SubscriptionMapper subscriptionMapper;

    /**
     * Create subscription for the newly registered company using the specified plan code.
     *
     * @param companyId the new company id
     * @param planCode  plan code from registration request (must be a valid global plan)
     * @throws AppException if planCode is invalid with message "Plan này không hợp lệ"
     */
    public void createSubscriptionForCompany(String companyId, String planCode) {
        if (!StringUtils.hasText(companyId)) return;
        if (!StringUtils.hasText(planCode)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "planCode is required");
        }

        PlanEntity plan = findValidGlobalPlan(planCode.trim());
        if (plan == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "Plan này không hợp lệ");
        }

        Date now = new Date();
        SubscriptionEntity sub = new SubscriptionEntity();
        sub.setSubscriptionId(UuidGenerator.generate());
        sub.setCompanyId(companyId.trim());
        sub.setPlanId(plan.getPlanId());
        sub.setBillingCycle("MONTHLY");
        sub.setStatus("ACTIVE");
        sub.setCurrentPeriodStart(toDate(LocalDate.now()));
        sub.setCurrentPeriodEnd(toDate(LocalDate.now().plusMonths(1)));
        sub.setAutoRenew(false);
        sub.setCreatedAt(now);
        sub.setUpdatedAt(now);

        int inserted = subscriptionMapper.insert(sub);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create subscription failed");
        }
        log.info("Created subscription {} for company {} with plan {}", sub.getSubscriptionId(), companyId, planCode);
    }

    private PlanEntity findValidGlobalPlan(String planCode) {
        return planMapper.selectAll().stream()
                .filter(p -> p != null && planCode.equalsIgnoreCase(p.getCode()))
                .filter(p -> !StringUtils.hasText(p.getCompanyId()))
                .filter(p -> "ACTIVE".equalsIgnoreCase(p.getStatus()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Create FREE plan subscription. Used by onboarding.company.setup flow.
     */
    public void createFreeSubscriptionForCompany(String companyId) {
        if (!StringUtils.hasText(companyId)) return;
        PlanEntity freePlan = findValidGlobalPlan("FREE");
        if (freePlan == null) {
            log.warn("FREE plan not found - skipping subscription creation for company {}", companyId);
            return;
        }
        createSubscriptionForCompany(companyId, "FREE");
    }

    private static Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
