package com.sme.be_sme.modules.billing.service;

import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentAttachmentMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentVersionMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.EventTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskAttachmentMapper;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CompanyPlanQuotaService {

    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;
    private final OnboardingTemplateMapper onboardingTemplateMapper;
    private final EventTemplateMapper eventTemplateMapper;
    private final DocumentMapper documentMapper;
    private final DocumentVersionMapper documentVersionMapper;
    private final TaskAttachmentMapper taskAttachmentMapper;
    private final DocumentAttachmentMapper documentAttachmentMapper;

    public void assertCanCreateOnboardingTemplate(String companyId) {
        PlanEntity plan = resolveCurrentPlan(companyId);
        if (plan == null || plan.getOnboardingTemplateLimit() == null || plan.getOnboardingTemplateLimit() <= 0) {
            return;
        }
        long count = onboardingTemplateMapper.countTenantTemplatesByCompanyId(companyId);
        if (count >= plan.getOnboardingTemplateLimit()) {
            throw AppException.of(ErrorCodes.LIMIT_EXCEEDED, "onboarding template limit reached");
        }
    }

    public void assertCanCreateEventTemplate(String companyId) {
        PlanEntity plan = resolveCurrentPlan(companyId);
        if (plan == null || plan.getEventTemplateLimit() == null || plan.getEventTemplateLimit() <= 0) {
            return;
        }
        long count = eventTemplateMapper.countByCompanyId(companyId);
        if (count >= plan.getEventTemplateLimit()) {
            throw AppException.of(ErrorCodes.LIMIT_EXCEEDED, "event template limit reached");
        }
    }

    public void assertCanCreateDocument(String companyId) {
        PlanEntity plan = resolveCurrentPlan(companyId);
        if (plan == null || plan.getDocumentLimit() == null || plan.getDocumentLimit() <= 0) {
            return;
        }
        long count = documentMapper.countByCompanyId(companyId);
        if (count >= plan.getDocumentLimit()) {
            throw AppException.of(ErrorCodes.LIMIT_EXCEEDED, "document limit reached");
        }
    }

    public void assertStorageWithinLimit(String companyId) {
        assertCanAddStorage(companyId, 0L);
    }

    public void assertCanAddStorage(String companyId, Long additionalBytes) {
        long incoming = normalizeBytes(additionalBytes);
        PlanEntity plan = resolveCurrentPlan(companyId);
        if (plan == null || plan.getStorageLimitBytes() == null || plan.getStorageLimitBytes() <= 0) {
            return;
        }
        long current = getCurrentStorageBytes(companyId);
        if (current + incoming > plan.getStorageLimitBytes()) {
            throw AppException.of(ErrorCodes.LIMIT_EXCEEDED, "storage limit reached");
        }
    }

    public long getCurrentStorageBytes(String companyId) {
        if (!StringUtils.hasText(companyId)) {
            return 0L;
        }
        Long taskBytes = taskAttachmentMapper.sumFileSizeBytesByCompanyId(companyId.trim());
        Long docBytes = documentAttachmentMapper.sumFileSizeBytesByCompanyId(companyId.trim());
        Long docVersionBytes = documentVersionMapper.sumFileSizeBytesByCompanyId(companyId.trim());
        long task = taskBytes == null ? 0L : taskBytes;
        long doc = docBytes == null ? 0L : docBytes;
        long docVersions = docVersionBytes == null ? 0L : docVersionBytes;
        return task + doc + docVersions;
    }

    public long getCurrentOnboardingTemplateCount(String companyId) {
        if (!StringUtils.hasText(companyId)) {
            return 0L;
        }
        return onboardingTemplateMapper.countTenantTemplatesByCompanyId(companyId.trim());
    }

    public long getCurrentEventTemplateCount(String companyId) {
        if (!StringUtils.hasText(companyId)) {
            return 0L;
        }
        return eventTemplateMapper.countByCompanyId(companyId.trim());
    }

    public long getCurrentDocumentCount(String companyId) {
        if (!StringUtils.hasText(companyId)) {
            return 0L;
        }
        return documentMapper.countByCompanyId(companyId.trim());
    }

    private PlanEntity resolveCurrentPlan(String companyId) {
        if (!StringUtils.hasText(companyId)) {
            return null;
        }
        List<SubscriptionEntity> list = subscriptionMapper.selectAll();
        if (list == null) {
            return null;
        }
        SubscriptionEntity current = list.stream()
                .filter(Objects::nonNull)
                .filter(s -> companyId.trim().equals(s.getCompanyId()))
                .filter(s -> "ACTIVE".equalsIgnoreCase(trimLower(s.getStatus())))
                .max(Comparator.comparing(SubscriptionEntity::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        if (current == null || !StringUtils.hasText(current.getPlanId())) {
            return null;
        }
        return planMapper.selectByPrimaryKey(current.getPlanId().trim());
    }

    private static long normalizeBytes(Long value) {
        if (value == null || value <= 0L) {
            return 0L;
        }
        return value;
    }

    private static String trimLower(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
