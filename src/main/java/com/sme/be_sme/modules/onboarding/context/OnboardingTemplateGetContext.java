package com.sme.be_sme.modules.onboarding.context;

import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateGetRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateGetResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.model.ChecklistTemplateRow;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.model.TaskTemplateRow;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OnboardingTemplateGetContext {
    private BizContext biz;
    private OnboardingTemplateGetRequest request;
    private OnboardingTemplateGetResponse response;

    private OnboardingTemplateEntity template;
    private List<ChecklistTemplateRow> checklistRows;
    private List<TaskTemplateRow> baselineTaskRows;
}
