package com.sme.be_sme.modules.billing.service;

import com.sme.be_sme.modules.billing.api.request.PlanGetRequest;
import com.sme.be_sme.modules.billing.api.response.PlanGetResponse;
import com.sme.be_sme.modules.billing.facade.BillingFacade;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentAttachmentMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.EventTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskAttachmentMapper;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CompanyPlanQuotaService {

    private final BillingFacade billingFacade;
    private final OnboardingTemplateMapper onboardingTemplateMapper;
    private final EventTemplateMapper eventTemplateMapper;
    private final DocumentMapper documentMapper;
    private final TaskAttachmentMapper taskAttachmentMapper;
    private final DocumentAttachmentMapper documentAttachmentMapper;

    public void assertCanCreateOnboardingTemplate(String companyId) {
        PlanGetResponse plan = resolveCurrentPlan();
        if (plan == null || plan.getOnboardingTemplateLimit() == null || plan.getOnboardingTemplateLimit() <= 0) {
            return;
        }
        long count = onboardingTemplateMapper.countTenantTemplatesByCompanyId(companyId);
        if (count >= plan.getOnboardingTemplateLimit()) {
            throw AppException.of(ErrorCodes.LIMIT_EXCEEDED, "onboarding template limit reached");
        }
    }

    public void assertCanCreateEventTemplate(String companyId) {
        PlanGetResponse plan = resolveCurrentPlan();
        if (plan == null || plan.getEventTemplateLimit() == null || plan.getEventTemplateLimit() <= 0) {
            return;
        }
        long count = eventTemplateMapper.countByCompanyId(companyId);
        if (count >= plan.getEventTemplateLimit()) {
            throw AppException.of(ErrorCodes.LIMIT_EXCEEDED, "event template limit reached");
        }
    }

    public void assertCanCreateDocument(String companyId) {
        PlanGetResponse plan = resolveCurrentPlan();
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
        PlanGetResponse plan = resolveCurrentPlan();
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
        long task = taskBytes == null ? 0L : taskBytes;
        long doc = docBytes == null ? 0L : docBytes;
        return task + doc;
    }

    private PlanGetResponse resolveCurrentPlan() {
        try {
            return billingFacade.getPlan(new PlanGetRequest());
        } catch (Exception e) {
            return null;
        }
    }

    private static long normalizeBytes(Long value) {
        if (value == null || value <= 0L) {
            return 0L;
        }
        return value;
    }
}
