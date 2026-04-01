package com.sme.be_sme.modules.automation.job;

import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapperExt;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskWorkflowNotificationService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskOverdueEscalationJob {

    private final TaskInstanceMapperExt taskInstanceMapperExt;
    private final OnboardingTaskWorkflowNotificationService workflowNotificationService;

    @Scheduled(cron = "${app.automation.task-overdue.cron:0 30 9 * * ?}")
    public void run() {
        Date today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<TaskInstanceEntity> tasks = taskInstanceMapperExt.selectOverdueAndStatusNotDone(today);
        if (tasks != null && !tasks.isEmpty()) {
            log.info("TaskOverdueEscalationJob: processing {} overdue tasks", tasks.size());
            for (TaskInstanceEntity task : tasks) {
                try {
                    workflowNotificationService.notifyOverdue(task, task.getCompanyId());
                } catch (Exception e) {
                    log.warn("TaskOverdueEscalationJob: failed for task {}: {}", task.getTaskId(), e.getMessage());
                }
            }
        }

        Date now = new Date();
        List<TaskInstanceEntity> noShowCandidates =
                taskInstanceMapperExt.selectScheduledStartedBeforeAndStatusNotDone(now);
        if (noShowCandidates == null || noShowCandidates.isEmpty()) {
            return;
        }
        log.info("TaskOverdueEscalationJob: processing {} no-show candidates", noShowCandidates.size());
        for (TaskInstanceEntity task : noShowCandidates) {
            try {
                workflowNotificationService.notifyNoShowCandidate(task, task.getCompanyId());
            } catch (Exception e) {
                log.warn("TaskOverdueEscalationJob: no-show notify failed for task {}: {}", task.getTaskId(), e.getMessage());
            }
        }
    }
}

