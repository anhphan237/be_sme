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

        // baseline tasks (flat list for backward compatibility)
        List<TaskTemplateRow> baselineTaskRows = ctx.getBaselineTaskRows() == null ? List.of() : ctx.getBaselineTaskRows();
        List<OnboardingTemplateGetResponse.TaskTemplateItemResponse> taskItems =
                baselineTaskRows.stream().map(this::mapTask).toList();
        res.setBaselineTasks(taskItems);

        // checklists with nested tasks
        List<OnboardingTemplateGetResponse.ChecklistTemplateItemResponse> checklistItems =
                ctx.getChecklistRows() == null ? List.of()
                        : ctx.getChecklistRows().stream()
                        .map(c -> mapChecklistWithTasks(c, baselineTaskRows))
                        .toList();
        res.setChecklists(checklistItems);

        return null;
    }

    private OnboardingTemplateGetResponse.ChecklistTemplateItemResponse mapChecklistWithTasks(
            ChecklistTemplateRow r, List<TaskTemplateRow> allTasks) {
        OnboardingTemplateGetResponse.ChecklistTemplateItemResponse x =
                new OnboardingTemplateGetResponse.ChecklistTemplateItemResponse();
        x.setChecklistTemplateId(r.getChecklistTemplateId());
        x.setName(r.getName());
        x.setStage(r.getStage());
        x.setOrderNo(r.getOrderNo());
        x.setStatus(r.getStatus());
        List<OnboardingTemplateGetResponse.TaskTemplateItemResponse> tasksOfChecklist = allTasks.stream()
                .filter(t -> r.getChecklistTemplateId() != null && r.getChecklistTemplateId().equals(t.getChecklistTemplateId()))
                .map(this::mapTask)
                .toList();
        x.setTasks(tasksOfChecklist);
        return x;
    }

    private OnboardingTemplateGetResponse.TaskTemplateItemResponse mapTask(TaskTemplateRow r) {
        OnboardingTemplateGetResponse.TaskTemplateItemResponse x =
                new OnboardingTemplateGetResponse.TaskTemplateItemResponse();
        x.setTaskTemplateId(r.getTaskTemplateId());
        x.setChecklistTemplateId(r.getChecklistTemplateId());
        x.setName(r.getName());
        x.setDescription(r.getDescription());
        x.setOwnerType(r.getOwnerType());
        x.setOwnerRefId(r.getOwnerRefId());
        x.setDueDaysOffset(r.getDueDaysOffset());
        x.setRequireAck(r.getRequireAck());
        x.setOrderNo(r.getOrderNo());
        x.setStatus(r.getStatus());
        return x;
    }
}
