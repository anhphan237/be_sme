package com.sme.be_sme.modules.onboarding.context;

import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceCreateRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.model.ChecklistTemplateRow;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.model.TaskTemplateRow;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OnboardingInstanceCreateContext {
    private BizContext biz;
    private OnboardingInstanceCreateRequest request;
    private OnboardingInstanceResponse response;

    private OnboardingTemplateEntity template;

    private String instanceId;
    /** Set when idempotency hit (existing instance with same requestNo). */
    private OnboardingInstanceEntity existingInstance;

    // rows from template
    private List<ChecklistTemplateRow> checklistRows;
    private List<TaskTemplateRow> baselineTaskRows;
}
