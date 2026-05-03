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
     * Creates an ACTIVE FREE global subscription after company registration (until paid upgrade applies).
     *
     * @param billingCycleOrNull MONTHLY or YEARLY; defaults to MONTHLY
     * @return subscriptionId
     */
    public String createFreeSubscriptionReturningId(String companyId, String billingCycleOrNull) {
        return insertGlobalPlanSubscriptionForCompany(companyId, "FREE", billingCycleOrNull);
    }

    /**
     * Create subscription using a global ACTIVE plan code (typically {@code FREE} from onboarding.company.setup).
     */
    public void createSubscriptionForCompany(String companyId, String planCode) {
        insertGlobalPlanSubscriptionForCompany(companyId, planCode, "MONTHLY");
    }

    /**
     * @return subscription id
     */
    private String insertGlobalPlanSubscriptionForCompany(String companyId, String planCode, String billingCycleOrNull) {
        if (!StringUtils.hasText(companyId)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "companyId is required");
        }
        if (!StringUtils.hasText(planCode)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "planCode is required");
        }

        PlanEntity plan = findValidGlobalPlan(planCode.trim());
        if (plan == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "Plan này không hợp lệ");
        }

        String cycle = normalizeBillingCycle(billingCycleOrNull);

        Date now = new Date();
        SubscriptionEntity sub = new SubscriptionEntity();
        sub.setSubscriptionId(UuidGenerator.generate());
        sub.setCompanyId(companyId.trim());
        sub.setPlanId(plan.getPlanId());
        sub.setBillingCycle(cycle);
        sub.setStatus("ACTIVE");
        sub.setCurrentPeriodStart(toDate(LocalDate.now()));
        if ("YEARLY".equalsIgnoreCase(cycle)) {
            sub.setCurrentPeriodEnd(toDate(LocalDate.now().plusYears(1)));
        } else {
            sub.setCurrentPeriodEnd(toDate(LocalDate.now().plusMonths(1)));
        }
        sub.setAutoRenew(false);
        sub.setCreatedAt(now);
        sub.setUpdatedAt(now);

        int inserted = subscriptionMapper.insert(sub);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create subscription failed");
        }
        log.info("Created subscription {} for company {} with plan {} cycle {}", sub.getSubscriptionId(), companyId, planCode, cycle);
        return sub.getSubscriptionId();
    }

    private static String normalizeBillingCycle(String billingCycleOrNull) {
        if (!StringUtils.hasText(billingCycleOrNull)) {
            return "MONTHLY";
        }
        String c = billingCycleOrNull.trim().toUpperCase();
        if ("YEARLY".equals(c)) {
            return "YEARLY";
        }
        return "MONTHLY";
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
