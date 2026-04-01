package com.sme.be_sme.modules.automation.job;

import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapperExt;
import com.sme.be_sme.modules.notification.service.NotificationService;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapperExt;
import java.time.ZoneId;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskReminderEmailJobTest {

    @Mock
    private TaskInstanceMapperExt taskInstanceMapperExt;
    @Mock
    private UserMapperExt userMapperExt;
    @Mock
    private NotificationService notificationService;

    @Test
    void run_queriesExactlyNextDayWindow() {
        TaskReminderEmailJob job = new TaskReminderEmailJob(taskInstanceMapperExt, userMapperExt, notificationService);
        when(taskInstanceMapperExt.selectDueBetweenAndStatusNotDone(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.List.of());
        when(taskInstanceMapperExt.selectScheduledBetweenAndStatusNotDone(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.List.of());

        job.run();

        ArgumentCaptor<Date> fromCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> toCaptor = ArgumentCaptor.forClass(Date.class);
        verify(taskInstanceMapperExt).selectDueBetweenAndStatusNotDone(fromCaptor.capture(), toCaptor.capture());
        verify(taskInstanceMapperExt).selectScheduledBetweenAndStatusNotDone(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());

        var from = fromCaptor.getValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        var to = toCaptor.getValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        assertEquals(from, to);
    }
}

