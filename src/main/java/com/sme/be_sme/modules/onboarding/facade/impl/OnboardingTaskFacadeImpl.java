package com.sme.be_sme.modules.onboarding.facade.impl;

import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskAcknowledgeRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskApproveRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskAssignRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskGenerateRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskRejectRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskUpdateStatusRequest;
import com.sme.be_sme.modules.onboarding.api.request.TaskAttachmentAddRequest;
import com.sme.be_sme.modules.onboarding.api.request.TaskDetailRequest;
import com.sme.be_sme.modules.onboarding.api.request.TaskListByAssigneeRequest;
import com.sme.be_sme.modules.onboarding.api.request.TaskListByOnboardingRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskGenerationResponse;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskResponse;
import com.sme.be_sme.modules.onboarding.api.response.TaskAttachmentAddResponse;
import com.sme.be_sme.modules.onboarding.api.response.TaskDetailResponse;
import com.sme.be_sme.modules.onboarding.api.response.TaskListByAssigneeResponse;
import com.sme.be_sme.modules.onboarding.api.response.TaskListByOnboardingResponse;
import com.sme.be_sme.modules.onboarding.facade.OnboardingTaskFacade;
import com.sme.be_sme.modules.onboarding.processor.OnboardingTaskAcknowledgeProcessor;
import com.sme.be_sme.modules.onboarding.processor.OnboardingTaskApproveProcessor;
import com.sme.be_sme.modules.onboarding.processor.OnboardingTaskAssignProcessor;
import com.sme.be_sme.modules.onboarding.processor.OnboardingTaskGenerateProcessor;
import com.sme.be_sme.modules.onboarding.processor.OnboardingTaskRejectProcessor;
import com.sme.be_sme.modules.onboarding.processor.OnboardingTaskUpdateStatusProcessor;
import com.sme.be_sme.modules.onboarding.processor.TaskAttachmentAddProcessor;
import com.sme.be_sme.modules.onboarding.processor.TaskDetailProcessor;
import com.sme.be_sme.modules.onboarding.processor.TaskListByAssigneeProcessor;
import com.sme.be_sme.modules.onboarding.processor.TaskListByOnboardingProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingTaskFacadeImpl extends BaseOperationFacade implements OnboardingTaskFacade {

    private final OnboardingTaskGenerateProcessor onboardingTaskGenerateProcessor;
    private final OnboardingTaskAssignProcessor onboardingTaskAssignProcessor;
    private final OnboardingTaskUpdateStatusProcessor onboardingTaskUpdateStatusProcessor;
    private final OnboardingTaskAcknowledgeProcessor onboardingTaskAcknowledgeProcessor;
    private final OnboardingTaskApproveProcessor onboardingTaskApproveProcessor;
    private final OnboardingTaskRejectProcessor onboardingTaskRejectProcessor;
    private final TaskAttachmentAddProcessor taskAttachmentAddProcessor;
    private final TaskListByOnboardingProcessor taskListByOnboardingProcessor;
    private final TaskListByAssigneeProcessor taskListByAssigneeProcessor;
    private final TaskDetailProcessor taskDetailProcessor;

    @Override
    public OnboardingTaskGenerationResponse generateTasksFromTemplate(OnboardingTaskGenerateRequest request) {
        return call(onboardingTaskGenerateProcessor, request, OnboardingTaskGenerationResponse.class);
    }

    @Override
    public OnboardingTaskResponse assignTask(OnboardingTaskAssignRequest request) {
        return call(onboardingTaskAssignProcessor, request, OnboardingTaskResponse.class);
    }

    @Override
    public OnboardingTaskResponse updateTaskStatus(OnboardingTaskUpdateStatusRequest request) {
        return call(onboardingTaskUpdateStatusProcessor, request, OnboardingTaskResponse.class);
    }

    @Override
    public OnboardingTaskResponse acknowledgeTask(OnboardingTaskAcknowledgeRequest request) {
        return call(onboardingTaskAcknowledgeProcessor, request, OnboardingTaskResponse.class);
    }

    @Override
    public OnboardingTaskResponse approveTask(OnboardingTaskApproveRequest request) {
        return call(onboardingTaskApproveProcessor, request, OnboardingTaskResponse.class);
    }

    @Override
    public OnboardingTaskResponse rejectTask(OnboardingTaskRejectRequest request) {
        return call(onboardingTaskRejectProcessor, request, OnboardingTaskResponse.class);
    }

    @Override
    public TaskAttachmentAddResponse addTaskAttachment(TaskAttachmentAddRequest request) {
        return call(taskAttachmentAddProcessor, request, TaskAttachmentAddResponse.class);
    }

    @Override
    public TaskListByOnboardingResponse listTasksByOnboarding(TaskListByOnboardingRequest request) {
        return call(taskListByOnboardingProcessor, request, TaskListByOnboardingResponse.class);
    }

    @Override
    public TaskListByAssigneeResponse listTasksByAssignee(TaskListByAssigneeRequest request) {
        return call(taskListByAssigneeProcessor, request, TaskListByAssigneeResponse.class);
    }

    @Override
    public TaskDetailResponse getTaskDetail(TaskDetailRequest request) {
        return call(taskDetailProcessor, request, TaskDetailResponse.class);
    }
}
