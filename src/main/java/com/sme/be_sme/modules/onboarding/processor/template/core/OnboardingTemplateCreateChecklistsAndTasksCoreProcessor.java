package com.sme.be_sme.modules.onboarding.processor.template.core;

import com.sme.be_sme.modules.onboarding.api.request.ChecklistTemplateCreateItem;
import com.sme.be_sme.modules.onboarding.api.request.TaskTemplateCreateItem;
import com.sme.be_sme.modules.onboarding.context.OnboardingTemplateCreateContext;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.ChecklistTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskTemplateRequiredDocumentMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.ChecklistTemplateEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskTemplateRequiredDocumentEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskTemplateEntity;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.util.UuidGenerator;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OnboardingTemplateCreateChecklistsAndTasksCoreProcessor extends BaseCoreProcessor<OnboardingTemplateCreateContext> {

    private final ChecklistTemplateMapper checklistTemplateMapper;
    private final TaskTemplateMapper taskTemplateMapper;
    private final TaskTemplateRequiredDocumentMapper taskTemplateRequiredDocumentMapper;
    private final DocumentMapper documentMapper;

    @Override
    protected Object process(OnboardingTemplateCreateContext ctx) {
        if (CollectionUtils.isEmpty(ctx.getRequest().getChecklists())) {
            return null;
        }
        String companyId = ctx.getCompanyId();
        String templateId = ctx.getTemplateId();
        var now = ctx.getNow();

        int checklistSortOrder = 0;
        for (ChecklistTemplateCreateItem checklistItem : ctx.getRequest().getChecklists()) {
            String checklistTemplateId = UuidGenerator.generate();
            ChecklistTemplateEntity checklistEntity = new ChecklistTemplateEntity();
            checklistEntity.setChecklistTemplateId(checklistTemplateId);
            checklistEntity.setCompanyId(companyId);
            checklistEntity.setOnboardingTemplateId(templateId);
            checklistEntity.setName(StringUtils.hasText(checklistItem.getName()) ? checklistItem.getName().trim() : "Checklist");
            checklistEntity.setStage(checklistItem.getStage());
            checklistEntity.setDeadlineDays(checklistItem.getDeadlineDays());
            checklistEntity.setSortOrder(checklistItem.getSortOrder() != null ? checklistItem.getSortOrder() : checklistSortOrder);
            checklistEntity.setStatus(StringUtils.hasText(checklistItem.getStatus()) ? checklistItem.getStatus() : "ACTIVE");
            checklistEntity.setCreatedAt(now);
            checklistEntity.setUpdatedAt(now);
            if (checklistTemplateMapper.insert(checklistEntity) != 1) {
                throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create checklist template failed");
            }
            checklistSortOrder++;

            if (!CollectionUtils.isEmpty(checklistItem.getTasks())) {
                int taskSortOrder = 0;
                for (TaskTemplateCreateItem taskItem : checklistItem.getTasks()) {
                    if (!StringUtils.hasText(taskItem.getTitle())) {
                        continue;
                    }
                    String taskTemplateId = UuidGenerator.generate();
                    TaskTemplateEntity taskEntity = new TaskTemplateEntity();
                    taskEntity.setTaskTemplateId(taskTemplateId);
                    taskEntity.setCompanyId(companyId);
                    taskEntity.setChecklistTemplateId(checklistTemplateId);
                    taskEntity.setTitle(taskItem.getTitle().trim());
                    taskEntity.setDescription(taskItem.getDescription());
                    taskEntity.setOwnerType(taskItem.getOwnerType());
                    taskEntity.setOwnerRefId(taskItem.getOwnerRefId());
                    taskEntity.setDueDaysOffset(taskItem.getDueDaysOffset());
                    boolean requireAck = Boolean.TRUE.equals(taskItem.getRequireAck());
                    List<String> requiredDocumentIds = normalizeRequiredDocumentIds(taskItem.getRequiredDocumentIds());
                    assertRequiredDocumentConfig(companyId, taskItem, requireAck, requiredDocumentIds);
                    taskEntity.setRequireAck(requireAck);
                    taskEntity.setRequireDoc(Boolean.TRUE.equals(taskItem.getRequireDoc()));
                    taskEntity.setRequiresManagerApproval(Boolean.TRUE.equals(taskItem.getRequiresManagerApproval()));
                    taskEntity.setApproverUserId(
                            StringUtils.hasText(taskItem.getApproverUserId()) ? taskItem.getApproverUserId().trim() : null);
                    taskEntity.setSortOrder(taskItem.getSortOrder() != null ? taskItem.getSortOrder() : taskSortOrder);
                    taskEntity.setStatus(StringUtils.hasText(taskItem.getStatus()) ? taskItem.getStatus() : "ACTIVE");
                    taskEntity.setCreatedAt(now);
                    taskEntity.setUpdatedAt(now);
                    if (taskTemplateMapper.insert(taskEntity) != 1) {
                        throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create task template failed");
                    }
                    for (String documentId : requiredDocumentIds) {
                        TaskTemplateRequiredDocumentEntity link = new TaskTemplateRequiredDocumentEntity();
                        link.setTaskTemplateRequiredDocumentId(UuidGenerator.generate());
                        link.setCompanyId(companyId);
                        link.setTaskTemplateId(taskTemplateId);
                        link.setDocumentId(documentId);
                        link.setCreatedAt(now);
                        if (taskTemplateRequiredDocumentMapper.insert(link) != 1) {
                            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "attach required document to task template failed");
                        }
                    }
                    taskSortOrder++;
                }
            }
        }
        return null;
    }

    private void assertRequiredDocumentConfig(
            String companyId,
            TaskTemplateCreateItem taskItem,
            boolean requireAck,
            List<String> requiredDocumentIds) {
        if (requireAck && CollectionUtils.isEmpty(requiredDocumentIds)) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "requiredDocumentIds is required when requireAck=true for task " + taskItem.getTitle());
        }
        for (String documentId : requiredDocumentIds) {
            DocumentEntity document = documentMapper.selectByPrimaryKey(documentId);
            if (document == null || !companyId.equals(document.getCompanyId())) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid documentId: " + documentId);
            }
            if (StringUtils.hasText(document.getStatus())
                    && !"ACTIVE".equalsIgnoreCase(document.getStatus().trim())) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "document is not ACTIVE: " + documentId);
            }
        }
    }

    private static List<String> normalizeRequiredDocumentIds(List<String> requiredDocumentIds) {
        if (CollectionUtils.isEmpty(requiredDocumentIds)) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String documentId : requiredDocumentIds) {
            if (StringUtils.hasText(documentId)) {
                normalized.add(documentId.trim());
            }
        }
        return List.copyOf(normalized);
    }
}
