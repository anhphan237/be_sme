package com.sme.be_sme.modules.onboarding.processor.template.core;

import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateGetResponse;
import com.sme.be_sme.modules.onboarding.context.OnboardingTemplateGetContext;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.model.ChecklistTemplateRow;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.model.TaskTemplateRow;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

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
        res.setTemplateKind(ctx.getTemplate().getTemplateKind());
        res.setDepartmentTypeCode(ctx.getTemplate().getDepartmentTypeCode());
        res.setLevel(ctx.getTemplate().getLevel());

        // baseline tasks (flat list for backward compatibility)
        List<TaskTemplateRow> baselineTaskRows = ctx.getBaselineTaskRows() == null ? List.of() : ctx.getBaselineTaskRows();
        Map<String, List<String>> requiredDocsByTaskTemplateId =
                ctx.getRequiredDocumentIdsByTaskTemplateId() == null ? Map.of() : ctx.getRequiredDocumentIdsByTaskTemplateId();
        Map<String, List<String>> responsibleDepartmentIdsByTaskTemplateId =
                ctx.getResponsibleDepartmentIdsByTaskTemplateId() == null ? Map.of() : ctx.getResponsibleDepartmentIdsByTaskTemplateId();
        List<OnboardingTemplateGetResponse.TaskTemplateItemResponse> taskItems =
                baselineTaskRows.stream().map(r -> mapTask(r, requiredDocsByTaskTemplateId, responsibleDepartmentIdsByTaskTemplateId)).toList();
        res.setBaselineTasks(taskItems);

        // checklists with nested tasks
        List<OnboardingTemplateGetResponse.ChecklistTemplateItemResponse> checklistItems =
                ctx.getChecklistRows() == null ? List.of()
                        : ctx.getChecklistRows().stream()
                        .map(c -> mapChecklistWithTasks(c, baselineTaskRows, requiredDocsByTaskTemplateId, responsibleDepartmentIdsByTaskTemplateId))
                        .toList();
        res.setChecklists(checklistItems);

        return null;
    }

    private OnboardingTemplateGetResponse.ChecklistTemplateItemResponse mapChecklistWithTasks(
            ChecklistTemplateRow r,
            List<TaskTemplateRow> allTasks,
            Map<String, List<String>> requiredDocsByTaskTemplateId,
            Map<String, List<String>> responsibleDepartmentIdsByTaskTemplateId) {
        OnboardingTemplateGetResponse.ChecklistTemplateItemResponse x =
                new OnboardingTemplateGetResponse.ChecklistTemplateItemResponse();
        x.setChecklistTemplateId(r.getChecklistTemplateId());
        x.setName(r.getName());
        x.setStage(r.getStage());
        x.setDeadlineDays(r.getDeadlineDays());
        x.setOrderNo(r.getOrderNo());
        x.setStatus(r.getStatus());
        List<OnboardingTemplateGetResponse.TaskTemplateItemResponse> tasksOfChecklist = allTasks.stream()
                .filter(t -> r.getChecklistTemplateId() != null && r.getChecklistTemplateId().equals(t.getChecklistTemplateId()))
                .map(t -> mapTask(t, requiredDocsByTaskTemplateId, responsibleDepartmentIdsByTaskTemplateId))
                .toList();
        x.setTasks(tasksOfChecklist);
        return x;
    }

    private OnboardingTemplateGetResponse.TaskTemplateItemResponse mapTask(
            TaskTemplateRow r,
            Map<String, List<String>> requiredDocsByTaskTemplateId,
            Map<String, List<String>> responsibleDepartmentIdsByTaskTemplateId) {
        OnboardingTemplateGetResponse.TaskTemplateItemResponse x =
                new OnboardingTemplateGetResponse.TaskTemplateItemResponse();
        x.setTaskTemplateId(r.getTaskTemplateId());
        x.setChecklistTemplateId(r.getChecklistTemplateId());
        x.setName(r.getName());
        x.setDescription(r.getDescription());
        x.setOwnerType(r.getOwnerType());
        x.setOwnerRefId(r.getOwnerRefId());
        if (StringUtils.hasText(r.getOwnerType())
                && "DEPARTMENT".equalsIgnoreCase(r.getOwnerType().trim())) {
            x.setResponsibleDepartmentId(r.getOwnerRefId());
        }
        x.setDueDaysOffset(r.getDueDaysOffset());
        x.setRequireAck(r.getRequireAck());
        x.setRequireDoc(r.getRequireDoc());
        x.setRequiresManagerApproval(r.getRequiresManagerApproval());
        x.setApproverUserId(r.getApproverUserId());
        x.setRequiredDocumentIds(requiredDocsByTaskTemplateId.getOrDefault(r.getTaskTemplateId(), List.of()));
        x.setResponsibleDepartmentIds(responsibleDepartmentIdsByTaskTemplateId.getOrDefault(r.getTaskTemplateId(), List.of()));
        x.setOrderNo(r.getOrderNo());
        x.setStatus(r.getStatus());
        return x;
    }
}
