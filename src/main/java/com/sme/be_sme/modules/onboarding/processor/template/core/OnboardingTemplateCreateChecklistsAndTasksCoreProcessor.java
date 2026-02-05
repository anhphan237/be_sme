package com.sme.be_sme.modules.onboarding.processor.template.core;

import com.sme.be_sme.modules.onboarding.api.request.ChecklistTemplateCreateItem;
import com.sme.be_sme.modules.onboarding.api.request.TaskTemplateCreateItem;
import com.sme.be_sme.modules.onboarding.context.OnboardingTemplateCreateContext;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.ChecklistTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.ChecklistTemplateEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskTemplateEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OnboardingTemplateCreateChecklistsAndTasksCoreProcessor extends BaseCoreProcessor<OnboardingTemplateCreateContext> {

    private final ChecklistTemplateMapper checklistTemplateMapper;
    private final TaskTemplateMapper taskTemplateMapper;

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
                    taskEntity.setRequireAck(taskItem.getRequireAck());
                    taskEntity.setSortOrder(taskItem.getSortOrder() != null ? taskItem.getSortOrder() : taskSortOrder);
                    taskEntity.setStatus(StringUtils.hasText(taskItem.getStatus()) ? taskItem.getStatus() : "ACTIVE");
                    taskEntity.setCreatedAt(now);
                    taskEntity.setUpdatedAt(now);
                    if (taskTemplateMapper.insert(taskEntity) != 1) {
                        throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create task template failed");
                    }
                    taskSortOrder++;
                }
            }
        }
        return null;
    }
}
