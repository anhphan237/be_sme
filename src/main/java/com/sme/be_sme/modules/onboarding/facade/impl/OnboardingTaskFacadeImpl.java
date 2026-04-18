package com.sme.be_sme.modules.onboarding.facade.impl;

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
import com.sme.be_sme.modules.onboarding.facade.OnboardingTaskFacade;
import com.sme.be_sme.modules.onboarding.processor.OnboardingTaskAcknowledgeProcessor;
import com.sme.be_sme.modules.onboarding.processor.OnboardingTaskApproveProcessor;
import com.sme.be_sme.modules.onboarding.processor.OnboardingTaskAssignProcessor;
import com.sme.be_sme.modules.onboarding.processor.OnboardingTaskGenerateProcessor;
import com.sme.be_sme.modules.onboarding.processor.OnboardingTaskRejectProcessor;
import com.sme.be_sme.modules.onboarding.processor.OnboardingTaskUpdateStatusProcessor;
import com.sme.be_sme.modules.onboarding.processor.TaskAttachmentAddProcessor;
import com.sme.be_sme.modules.onboarding.processor.TaskCommentAddProcessor;
import com.sme.be_sme.modules.onboarding.processor.TaskDetailProcessor;
import com.sme.be_sme.modules.onboarding.processor.TaskListByAssigneeProcessor;
import com.sme.be_sme.modules.onboarding.processor.TaskListByOnboardingProcessor;
import com.sme.be_sme.modules.onboarding.processor.TaskScheduleConfirmProcessor;
import com.sme.be_sme.modules.onboarding.processor.TaskScheduleCancelProcessor;
import com.sme.be_sme.modules.onboarding.processor.TaskScheduleMarkNoShowProcessor;
import com.sme.be_sme.modules.onboarding.processor.TaskScheduleProposeProcessor;
import com.sme.be_sme.modules.onboarding.processor.TaskScheduleRescheduleProcessor;
import com.sme.be_sme.modules.onboarding.processor.TaskTimelineByOnboardingProcessor;
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
    private final TaskCommentAddProcessor taskCommentAddProcessor;
    private final TaskListByOnboardingProcessor taskListByOnboardingProcessor;
    private final TaskListByAssigneeProcessor taskListByAssigneeProcessor;
    private final TaskTimelineByOnboardingProcessor taskTimelineByOnboardingProcessor;
    private final TaskScheduleProposeProcessor taskScheduleProposeProcessor;
    private final TaskScheduleConfirmProcessor taskScheduleConfirmProcessor;
    private final TaskScheduleRescheduleProcessor taskScheduleRescheduleProcessor;
    private final TaskScheduleCancelProcessor taskScheduleCancelProcessor;
    private final TaskScheduleMarkNoShowProcessor taskScheduleMarkNoShowProcessor;
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
    public TaskCommentAddResponse addTaskComment(TaskCommentAddRequest request) {
        return call(taskCommentAddProcessor, request, TaskCommentAddResponse.class);
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
    public TaskTimelineByOnboardingResponse timelineByOnboarding(TaskTimelineByOnboardingRequest request) {
        return call(taskTimelineByOnboardingProcessor, request, TaskTimelineByOnboardingResponse.class);
    }

    @Override
    public TaskScheduleResponse proposeTaskSchedule(TaskScheduleProposeRequest request) {
        return call(taskScheduleProposeProcessor, request, TaskScheduleResponse.class);
    }

    @Override
    public TaskScheduleResponse confirmTaskSchedule(TaskScheduleConfirmRequest request) {
        return call(taskScheduleConfirmProcessor, request, TaskScheduleResponse.class);
    }

    @Override
    public TaskScheduleResponse rescheduleTask(TaskScheduleRescheduleRequest request) {
        return call(taskScheduleRescheduleProcessor, request, TaskScheduleResponse.class);
    }

    @Override
    public TaskScheduleResponse cancelTaskSchedule(TaskScheduleCancelRequest request) {
        return call(taskScheduleCancelProcessor, request, TaskScheduleResponse.class);
    }

    @Override
    public TaskScheduleResponse markTaskScheduleNoShow(TaskScheduleMarkNoShowRequest request) {
        return call(taskScheduleMarkNoShowProcessor, request, TaskScheduleResponse.class);
    }

    @Override
    public TaskDetailResponse getTaskDetail(TaskDetailRequest request) {
        return call(taskDetailProcessor, request, TaskDetailResponse.class);
    }
}
