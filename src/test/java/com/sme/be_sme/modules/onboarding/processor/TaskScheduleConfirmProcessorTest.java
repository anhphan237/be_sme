package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.response.TaskScheduleResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskActivityLogService;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskApprovalAuthority;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskWorkflowNotificationService;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskWorkflow;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskScheduleConfirmProcessorTest {

    @Mock
    private TaskInstanceMapper taskInstanceMapper;
    @Mock
    private OnboardingTaskApprovalAuthority approvalAuthority;
    @Mock
    private OnboardingTaskActivityLogService activityLogService;
    @Mock
    private OnboardingTaskWorkflowNotificationService workflowNotificationService;

    @Test
    void confirm_changesStatusToConfirmed() {
        TaskScheduleConfirmProcessor processor = new TaskScheduleConfirmProcessor(
                new ObjectMapper(),
                taskInstanceMapper,
                approvalAuthority,
                activityLogService,
                workflowNotificationService
        );
        BizContext context = new BizContext();
        context.setTenantId("c1");
        context.setOperatorId("u2");
        context.setRoles(Set.of("EMPLOYEE"));
        context.setPayload(new ObjectMapper().createObjectNode().put("taskId", "t1"));

        TaskInstanceEntity task = new TaskInstanceEntity();
        task.setTaskId("t1");
        task.setCompanyId("c1");
        task.setScheduleStatus(OnboardingTaskWorkflow.SCHEDULE_PROPOSED);
        task.setScheduleProposedBy("u1");
        task.setApproverUserId("u2");

        when(taskInstanceMapper.selectByPrimaryKey("t1")).thenReturn(task);
        when(taskInstanceMapper.updateByPrimaryKey(any(TaskInstanceEntity.class))).thenReturn(1);

        TaskScheduleResponse response = (TaskScheduleResponse) processor.execute(context);
        assertEquals(OnboardingTaskWorkflow.SCHEDULE_CONFIRMED, response.getScheduleStatus());
        verify(activityLogService).logScheduleConfirmed(any(), any(), any());
        verify(workflowNotificationService).notifyScheduleConfirmed(any(), any(), any());
    }
}

