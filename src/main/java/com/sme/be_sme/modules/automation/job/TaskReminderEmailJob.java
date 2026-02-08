package com.sme.be_sme.modules.automation.job;

import com.sme.be_sme.modules.automation.service.EmailSenderService;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapperExt;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.notification.infrastructure.mapper.NotificationMapper;
import com.sme.be_sme.modules.notification.infrastructure.persistence.entity.NotificationEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapperExt;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Daily job: send TASK_REMINDER email for tasks due in 1-2 days (status != DONE). Optionally create in-app notification.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskReminderEmailJob {

    private static final String TEMPLATE_TASK_REMINDER = "TASK_REMINDER";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String NOTIFICATION_TYPE_TASK_REMINDER = "TASK_REMINDER";

    private final TaskInstanceMapperExt taskInstanceMapperExt;
    private final UserMapperExt userMapperExt;
    private final EmailSenderService emailSenderService;
    private final NotificationMapper notificationMapper;

    @Scheduled(cron = "${app.automation.task-reminder.cron:0 0 9 * * ?}")
    public void run() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.plusDays(1);
        LocalDate to = today.plusDays(2);
        Date fromDate = Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date toDate = Date.from(to.atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<TaskInstanceEntity> tasks = taskInstanceMapperExt.selectDueBetweenAndStatusNotDone(fromDate, toDate);
        if (tasks == null || tasks.isEmpty()) return;
        log.info("TaskReminderEmailJob: sending {} task reminders", tasks.size());
        for (TaskInstanceEntity task : tasks) {
            try {
                sendReminder(task);
            } catch (Exception e) {
                log.warn("TaskReminderEmailJob: failed for task {}: {}", task.getTaskId(), e.getMessage());
            }
        }
    }

    private void sendReminder(TaskInstanceEntity task) {
        String assigneeUserId = task.getAssignedUserId();
        if (!StringUtils.hasText(assigneeUserId)) return;
        UserEntity user = userMapperExt.selectByCompanyIdAndUserId(task.getCompanyId(), assigneeUserId);
        if (user == null || !StringUtils.hasText(user.getEmail())) return;
        String dueStr = task.getDueDate() != null
                ? task.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FMT)
                : "";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("taskTitle", StringUtils.hasText(task.getTitle()) ? task.getTitle() : "Task");
        placeholders.put("dueDate", dueStr);
        emailSenderService.sendWithTemplate(task.getCompanyId(), TEMPLATE_TASK_REMINDER, user.getEmail(),
                placeholders, assigneeUserId, null);

        NotificationEntity n = new NotificationEntity();
        n.setNotificationId(UuidGenerator.generate());
        n.setCompanyId(task.getCompanyId());
        n.setUserId(assigneeUserId);
        n.setType(NOTIFICATION_TYPE_TASK_REMINDER);
        n.setTitle("Task due soon: " + (task.getTitle() != null ? task.getTitle() : "Task"));
        n.setContent("Task \"" + (task.getTitle() != null ? task.getTitle() : "Task") + "\" is due on " + dueStr + ".");
        n.setStatus("UNREAD");
        n.setRefType("TASK");
        n.setRefId(task.getTaskId());
        n.setCreatedAt(new Date());
        notificationMapper.insert(n);
    }
}
