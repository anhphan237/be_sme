package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.response.TaskScheduleResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskActivityLogService;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskApprovalAuthority;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskWorkflow;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
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
class TaskScheduleMarkNoShowProcessorTest {

    @Mock
    private TaskInstanceMapper taskInstanceMapper;
    @Mock
    private OnboardingTaskApprovalAuthority approvalAuthority;
    @Mock
    private OnboardingTaskActivityLogService activityLogService;

    @Test
    void markNoShow_setsMissedStatus() {
        TaskScheduleMarkNoShowProcessor processor = new TaskScheduleMarkNoShowProcessor(
                new ObjectMapper(),
                taskInstanceMapper,
                approvalAuthority,
                activityLogService
        );
        BizContext context = new BizContext();
        context.setTenantId("c1");
        context.setOperatorId("u1");
        context.setRoles(Set.of("EMPLOYEE"));
        context.setPayload(new ObjectMapper().createObjectNode()
                .put("taskId", "t1")
                .put("reason", "did not attend"));

        TaskInstanceEntity task = new TaskInstanceEntity();
        task.setTaskId("t1");
        task.setCompanyId("c1");
        task.setAssignedUserId("u1");
        task.setStatus(OnboardingTaskWorkflow.STATUS_TODO);
        task.setScheduledStartAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)));
        when(taskInstanceMapper.selectByPrimaryKey("t1")).thenReturn(task);
        when(taskInstanceMapper.updateByPrimaryKey(any(TaskInstanceEntity.class))).thenReturn(1);

        TaskScheduleResponse response = (TaskScheduleResponse) processor.execute(context);
        assertEquals(OnboardingTaskWorkflow.SCHEDULE_MISSED, response.getScheduleStatus());
        assertEquals("did not attend", response.getScheduleNoShowReason());
        verify(activityLogService).logScheduleNoShow(any(), any(), any());
    }
}

