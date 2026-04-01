package com.sme.be_sme.modules.onboarding.service;

import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskWorkflow;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.springframework.stereotype.Service;

@Service
public class OnboardingTaskSlaService {

    public String dueCategory(Date dueDate, String status) {
        if (isDone(status) || dueDate == null) {
            return "NONE";
        }
        long dueInHours = dueInHours(dueDate);
        if (dueInHours < 0) {
            return "OVERDUE";
        }
        if (dueInHours <= 24) {
            return "DUE_SOON";
        }
        return "ON_TRACK";
    }

    public boolean isOverdue(Date dueDate, String status) {
        return "OVERDUE".equals(dueCategory(dueDate, status));
    }

    public long dueInHours(Date dueDate) {
        if (dueDate == null) {
            return Long.MAX_VALUE;
        }
        Instant now = Instant.now();
        return Duration.between(now, dueDate.toInstant()).toHours();
    }

    public String dueCategory(TaskInstanceEntity task) {
        if (task == null) {
            return "NONE";
        }
        return dueCategory(task.getDueDate(), task.getStatus());
    }

    private static boolean isDone(String status) {
        return OnboardingTaskWorkflow.STATUS_DONE.equalsIgnoreCase(status == null ? "" : status);
    }
}

