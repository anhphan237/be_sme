package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceGetRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceDetailResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.ChecklistInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.ChecklistInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OnboardingInstanceGetProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final ChecklistInstanceMapper checklistInstanceMapper;
    private final TaskInstanceMapper taskInstanceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingInstanceGetRequest request = objectMapper.convertValue(payload, OnboardingInstanceGetRequest.class);
        validate(context, request);

        String companyId = context.getTenantId();
        OnboardingInstanceEntity instance = onboardingInstanceMapper.selectByPrimaryKey(request.getInstanceId().trim());
        if (instance == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "onboarding instance not found");
        }
        if (!companyId.equals(instance.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "instance does not belong to tenant");
        }

        OnboardingInstanceDetailResponse response = new OnboardingInstanceDetailResponse();
        response.setInstanceId(instance.getOnboardingId());
        response.setEmployeeId(instance.getEmployeeId());
        response.setTemplateId(instance.getOnboardingTemplateId());
        response.setStatus(instance.getStatus());
        response.setStartDate(instance.getStartDate());
        response.setCompletedAt(instance.getCompletedAt());
        response.setProgressPercent(instance.getProgressPercent() != null ? instance.getProgressPercent() : 0);

        List<ChecklistInstanceEntity> checklists = checklistInstanceMapper.selectByCompanyIdAndOnboardingId(
                companyId, instance.getOnboardingId());
        List<TaskInstanceEntity> allTasks = taskInstanceMapper.selectByCompanyIdAndOnboardingId(
                companyId, instance.getOnboardingId());

        List<OnboardingInstanceDetailResponse.ChecklistDetailItem> checklistItems = new ArrayList<>();
        if (checklists != null) {
            for (ChecklistInstanceEntity chk : checklists) {
                OnboardingInstanceDetailResponse.ChecklistDetailItem item = new OnboardingInstanceDetailResponse.ChecklistDetailItem();
                item.setChecklistId(chk.getChecklistId());
                item.setName(chk.getName());
                item.setStage(chk.getStage());
                item.setStatus(chk.getStatus());
                item.setProgressPercent(chk.getProgressPercent() != null ? chk.getProgressPercent() : 0);
                List<OnboardingInstanceDetailResponse.TaskDetailItem> taskItems = allTasks == null ? List.of() :
                        allTasks.stream()
                                .filter(t -> chk.getChecklistId().equals(t.getChecklistId()))
                                .map(this::toTaskDetailItem)
                                .collect(Collectors.toList());
                item.setTasks(taskItems);
                checklistItems.add(item);
            }
        }
        response.setChecklists(checklistItems);
        return response;
    }

    private OnboardingInstanceDetailResponse.TaskDetailItem toTaskDetailItem(TaskInstanceEntity t) {
        OnboardingInstanceDetailResponse.TaskDetailItem item = new OnboardingInstanceDetailResponse.TaskDetailItem();
        item.setTaskId(t.getTaskId());
        item.setTitle(t.getTitle());
        item.setStatus(t.getStatus());
        item.setAssignedUserId(t.getAssignedUserId());
        item.setDueDate(t.getDueDate());
        item.setCompletedAt(t.getCompletedAt());
        return item;
    }

    private static void validate(BizContext context, OnboardingInstanceGetRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getInstanceId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "instanceId is required");
        }
    }
}
