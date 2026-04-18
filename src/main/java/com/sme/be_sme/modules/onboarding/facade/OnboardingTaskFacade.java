package com.sme.be_sme.modules.onboarding.facade;

import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskAcknowledgeRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskApproveRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskAssignRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskGenerateRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskRejectRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskUpdateStatusRequest;
import com.sme.be_sme.modules.onboarding.api.request.TaskAttachmentAddRequest;
import com.sme.be_sme.modules.onboarding.api.request.TaskCommentAddRequest;
import com.sme.be_sme.modules.onboarding.api.request.TaskScheduleCancelRequest;
import com.sme.be_sme.modules.onboarding.api.request.TaskScheduleConfirmRequest;
import com.sme.be_sme.modules.onboarding.api.request.TaskScheduleMarkNoShowRequest;
import com.sme.be_sme.modules.onboarding.api.request.TaskScheduleProposeRequest;
import com.sme.be_sme.modules.onboarding.api.request.TaskScheduleRescheduleRequest;
import com.sme.be_sme.modules.onboarding.api.request.TaskDetailRequest;
import com.sme.be_sme.modules.onboarding.api.request.TaskListByAssigneeRequest;
import com.sme.be_sme.modules.onboarding.api.request.TaskListByOnboardingRequest;
import com.sme.be_sme.modules.onboarding.api.request.TaskTimelineByOnboardingRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskGenerationResponse;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskResponse;
import com.sme.be_sme.modules.onboarding.api.response.TaskAttachmentAddResponse;
import com.sme.be_sme.modules.onboarding.api.response.TaskCommentAddResponse;
import com.sme.be_sme.modules.onboarding.api.response.TaskDetailResponse;
import com.sme.be_sme.modules.onboarding.api.response.TaskListByAssigneeResponse;
import com.sme.be_sme.modules.onboarding.api.response.TaskListByOnboardingResponse;
import com.sme.be_sme.modules.onboarding.api.response.TaskScheduleResponse;
import com.sme.be_sme.modules.onboarding.api.response.TaskTimelineByOnboardingResponse;
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

    @OperationType("com.sme.onboarding.task.comment.add")
    TaskCommentAddResponse addTaskComment(TaskCommentAddRequest request);

    @OperationType("com.sme.onboarding.task.listByOnboarding")
    TaskListByOnboardingResponse listTasksByOnboarding(TaskListByOnboardingRequest request);

    @OperationType("com.sme.onboarding.task.listByAssignee")
    TaskListByAssigneeResponse listTasksByAssignee(TaskListByAssigneeRequest request);

    @OperationType("com.sme.onboarding.task.timelineByOnboarding")
    TaskTimelineByOnboardingResponse timelineByOnboarding(TaskTimelineByOnboardingRequest request);

    @OperationType("com.sme.onboarding.task.schedule.propose")
    TaskScheduleResponse proposeTaskSchedule(TaskScheduleProposeRequest request);

    @OperationType("com.sme.onboarding.task.schedule.confirm")
    TaskScheduleResponse confirmTaskSchedule(TaskScheduleConfirmRequest request);

    @OperationType("com.sme.onboarding.task.schedule.reschedule")
    TaskScheduleResponse rescheduleTask(TaskScheduleRescheduleRequest request);

    @OperationType("com.sme.onboarding.task.schedule.cancel")
    TaskScheduleResponse cancelTaskSchedule(TaskScheduleCancelRequest request);

    @OperationType("com.sme.onboarding.task.schedule.markNoShow")
    TaskScheduleResponse markTaskScheduleNoShow(TaskScheduleMarkNoShowRequest request);

    @OperationType("com.sme.onboarding.task.detail")
    TaskDetailResponse getTaskDetail(TaskDetailRequest request);
}
