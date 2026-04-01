package com.sme.be_sme.modules.automation.job;

import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapperExt;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskWorkflowNotificationService;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskOverdueEscalationJobTest {

    @Mock
    private TaskInstanceMapperExt taskInstanceMapperExt;
    @Mock
    private OnboardingTaskWorkflowNotificationService workflowNotificationService;

    @Test
    void run_sendsOverdueNotifications() {
        TaskOverdueEscalationJob job = new TaskOverdueEscalationJob(taskInstanceMapperExt, workflowNotificationService);

        TaskInstanceEntity task = new TaskInstanceEntity();
        task.setTaskId("t1");
        task.setCompanyId("c1");
        when(taskInstanceMapperExt.selectOverdueAndStatusNotDone(any(Date.class))).thenReturn(List.of(task));
        when(taskInstanceMapperExt.selectScheduledStartedBeforeAndStatusNotDone(any(Date.class))).thenReturn(List.of(task));

        job.run();

        verify(taskInstanceMapperExt).selectOverdueAndStatusNotDone(any(Date.class));
        verify(taskInstanceMapperExt).selectScheduledStartedBeforeAndStatusNotDone(any(Date.class));
        verify(workflowNotificationService).notifyOverdue(task, "c1");
        verify(workflowNotificationService).notifyNoShowCandidate(task, "c1");
    }
}

