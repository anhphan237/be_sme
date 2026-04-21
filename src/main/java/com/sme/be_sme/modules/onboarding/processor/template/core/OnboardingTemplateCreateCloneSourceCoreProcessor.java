package com.sme.be_sme.modules.onboarding.processor.template.core;

import com.sme.be_sme.modules.onboarding.api.request.ChecklistTemplateCreateItem;
import com.sme.be_sme.modules.onboarding.api.request.TaskTemplateCreateItem;
import com.sme.be_sme.modules.onboarding.context.OnboardingTemplateCreateContext;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingTemplateMapperExt;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskTemplateRequiredDocumentMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskTemplateRequiredDocumentEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.model.ChecklistTemplateRow;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.model.TaskTemplateRow;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OnboardingTemplateCreateCloneSourceCoreProcessor extends BaseCoreProcessor<OnboardingTemplateCreateContext> {

    private static final String LEVEL_PLATFORM = "PLATFORM";

    private final OnboardingTemplateMapperExt onboardingTemplateMapperExt;
    private final TaskTemplateRequiredDocumentMapper taskTemplateRequiredDocumentMapper;

    @Override
    protected Object process(OnboardingTemplateCreateContext ctx) {
        String sourceTemplateId = ctx.getRequest().getSourceTemplateId();
        if (!StringUtils.hasText(sourceTemplateId)) {
            return null;
        }

        String companyId = ctx.getCompanyId();
        OnboardingTemplateEntity source = onboardingTemplateMapperExt.selectTemplateByIdAndCompany(
                sourceTemplateId.trim(), companyId);
        if (source == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "source template not found");
        }
        if (!LEVEL_PLATFORM.equalsIgnoreCase(source.getLevel())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "only PLATFORM template can be cloned by sourceTemplateId");
        }
        if (!StringUtils.hasText(ctx.getRequest().getDescription())) {
            ctx.getRequest().setDescription(source.getDescription());
        }
        if (!StringUtils.hasText(ctx.getRequest().getTemplateKind())) {
            ctx.getRequest().setTemplateKind(source.getTemplateKind());
        }
        if (!StringUtils.hasText(ctx.getRequest().getDepartmentTypeCode())) {
            ctx.getRequest().setDepartmentTypeCode(source.getDepartmentTypeCode());
        }

        List<ChecklistTemplateRow> checklistRows = onboardingTemplateMapperExt.selectChecklistRows(
                source.getCompanyId(), source.getOnboardingTemplateId());
        List<TaskTemplateRow> taskRows = onboardingTemplateMapperExt.selectBaselineTaskRows(
                source.getCompanyId(), source.getOnboardingTemplateId());

        Map<String, List<String>> requiredDocumentIdsByTaskTemplateId = loadRequiredDocumentsByTaskTemplateId(source, taskRows);
        Map<String, List<TaskTemplateRow>> tasksByChecklist = taskRows.stream()
                .collect(Collectors.groupingBy(
                        TaskTemplateRow::getChecklistTemplateId,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<ChecklistTemplateCreateItem> clonedChecklists = checklistRows.stream()
                .map(row -> toChecklistItem(row, tasksByChecklist.getOrDefault(row.getChecklistTemplateId(), List.of()),
                        requiredDocumentIdsByTaskTemplateId))
                .toList();
        ctx.getRequest().setChecklists(clonedChecklists);
        return null;
    }

    private Map<String, List<String>> loadRequiredDocumentsByTaskTemplateId(
            OnboardingTemplateEntity source,
            List<TaskTemplateRow> taskRows) {
        List<String> taskTemplateIds = taskRows.stream()
                .map(TaskTemplateRow::getTaskTemplateId)
                .filter(StringUtils::hasText)
                .toList();
        if (taskTemplateIds.isEmpty()) {
            return Map.of();
        }
        return taskTemplateRequiredDocumentMapper
                .selectByCompanyIdAndTaskTemplateIds(source.getCompanyId(), taskTemplateIds)
                .stream()
                .collect(Collectors.groupingBy(
                        TaskTemplateRequiredDocumentEntity::getTaskTemplateId,
                        LinkedHashMap::new,
                        Collectors.mapping(TaskTemplateRequiredDocumentEntity::getDocumentId, Collectors.toList())));
    }

    private ChecklistTemplateCreateItem toChecklistItem(
            ChecklistTemplateRow row,
            List<TaskTemplateRow> taskRows,
            Map<String, List<String>> requiredDocumentIdsByTaskTemplateId) {
        ChecklistTemplateCreateItem item = new ChecklistTemplateCreateItem();
        item.setName(row.getName());
        item.setStage(row.getStage());
        item.setDeadlineDays(row.getDeadlineDays());
        item.setSortOrder(row.getOrderNo());
        item.setStatus(row.getStatus());
        item.setTasks(taskRows.stream()
                .map(taskRow -> toTaskItem(taskRow, requiredDocumentIdsByTaskTemplateId))
                .toList());
        return item;
    }

    private TaskTemplateCreateItem toTaskItem(
            TaskTemplateRow row,
            Map<String, List<String>> requiredDocumentIdsByTaskTemplateId) {
        TaskTemplateCreateItem item = new TaskTemplateCreateItem();
        item.setTitle(row.getName());
        item.setDescription(row.getDescription());
        item.setOwnerType(row.getOwnerType());
        item.setOwnerRefId(row.getOwnerRefId());
        item.setDueDaysOffset(row.getDueDaysOffset());
        item.setRequireAck(row.getRequireAck());
        item.setRequireDoc(row.getRequireDoc());
        item.setRequiresManagerApproval(row.getRequiresManagerApproval());
        item.setApproverUserId(row.getApproverUserId());
        item.setRequiredDocumentIds(requiredDocumentIdsByTaskTemplateId.getOrDefault(row.getTaskTemplateId(), List.of()));
        item.setSortOrder(row.getOrderNo());
        item.setStatus(row.getStatus());
        return item;
    }
}
