package com.sme.be_sme.modules.onboarding.facade;

import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskAcknowledgeRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskApproveRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskAssignRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskGenerateRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskRejectRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskUpdateStatusRequest;
import com.sme.be_sme.modules.onboarding.api.request.TaskAttachmentAddRequest;
import com.sme.be_sme.modules.onboarding.api.request.TaskDetailRequest;
import com.sme.be_sme.modules.onboarding.api.request.TaskListByOnboardingRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskGenerationResponse;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskResponse;
import com.sme.be_sme.modules.onboarding.api.response.TaskAttachmentAddResponse;
import com.sme.be_sme.modules.onboarding.api.response.TaskDetailResponse;
import com.sme.be_sme.modules.onboarding.api.response.TaskListByOnboardingResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface OnboardingTaskFacade extends OperationFacadeProvider {

    @OperationType("com.sme.onboarding.task.generate")
    OnboardingTaskGenerationResponse generateTasksFromTemplate(OnboardingTaskGenerateRequest request);

    @OperationType("com.sme.onboarding.task.assign")
    OnboardingTaskResponse assignTask(OnboardingTaskAssignRequest request);

    @OperationType("com.sme.onboarding.task.updateStatus")
    OnboardingTaskResponse updateTaskStatus(OnboardingTaskUpdateStatusRequest request);

    @OperationType("com.sme.onboarding.task.acknowledge")
    OnboardingTaskResponse acknowledgeTask(OnboardingTaskAcknowledgeRequest request);

    @OperationType("com.sme.onboarding.task.approve")
    OnboardingTaskResponse approveTask(OnboardingTaskApproveRequest request);

    @OperationType("com.sme.onboarding.task.reject")
    OnboardingTaskResponse rejectTask(OnboardingTaskRejectRequest request);

    @OperationType("com.sme.onboarding.task.attachment.add")
    TaskAttachmentAddResponse addTaskAttachment(TaskAttachmentAddRequest request);

    @OperationType("com.sme.onboarding.task.listByOnboarding")
    TaskListByOnboardingResponse listTasksByOnboarding(TaskListByOnboardingRequest request);

    @OperationType("com.sme.onboarding.task.detail")
    TaskDetailResponse getTaskDetail(TaskDetailRequest request);
}
