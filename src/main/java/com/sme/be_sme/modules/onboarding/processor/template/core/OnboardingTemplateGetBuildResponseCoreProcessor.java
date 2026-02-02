package com.sme.be_sme.modules.onboarding.processor.template.core;

import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateGetResponse;
import com.sme.be_sme.modules.onboarding.context.OnboardingTemplateGetContext;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.model.ChecklistTemplateRow;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.model.TaskTemplateRow;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OnboardingTemplateGetBuildResponseCoreProcessor extends BaseCoreProcessor<OnboardingTemplateGetContext> {

    @Override
    protected Object process(OnboardingTemplateGetContext ctx) {
        OnboardingTemplateGetResponse res = ctx.getResponse();

        // template info
        res.setTemplateId(ctx.getTemplate().getOnboardingTemplateId());
        res.setName(ctx.getTemplate().getName());
        res.setStatus(ctx.getTemplate().getStatus());
        res.setDescription(ctx.getTemplate().getDescription());

        // checklists
        List<OnboardingTemplateGetResponse.ChecklistTemplateItemResponse> checklistItems =
                ctx.getChecklistRows() == null ? List.of()
                        : ctx.getChecklistRows().stream().map(this::mapChecklist).toList();
        res.setChecklists(checklistItems);

        // baseline tasks
        List<OnboardingTemplateGetResponse.TaskTemplateItemResponse> taskItems =
                ctx.getBaselineTaskRows() == null ? List.of()
                        : ctx.getBaselineTaskRows().stream().map(this::mapTask).toList();
        res.setBaselineTasks(taskItems);

        return null;
    }

    private OnboardingTemplateGetResponse.ChecklistTemplateItemResponse mapChecklist(ChecklistTemplateRow r) {
        OnboardingTemplateGetResponse.ChecklistTemplateItemResponse x =
                new OnboardingTemplateGetResponse.ChecklistTemplateItemResponse();
        x.setChecklistTemplateId(r.getChecklistTemplateId());
        x.setName(r.getName());
        x.setDescription(r.getDescription());
        x.setOrderNo(r.getOrderNo());
        x.setStatus(r.getStatus());
        return x;
    }

    private OnboardingTemplateGetResponse.TaskTemplateItemResponse mapTask(TaskTemplateRow r) {
        OnboardingTemplateGetResponse.TaskTemplateItemResponse x =
                new OnboardingTemplateGetResponse.TaskTemplateItemResponse();
        x.setTaskTemplateId(r.getTaskTemplateId());
        x.setChecklistTemplateId(r.getChecklistTemplateId());
        x.setName(r.getName());
        x.setDescription(r.getDescription());
        x.setOrderNo(r.getOrderNo());
        x.setStatus(r.getStatus());
        return x;
    }
}
