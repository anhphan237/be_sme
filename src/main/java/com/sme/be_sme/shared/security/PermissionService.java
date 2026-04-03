package com.sme.be_sme.shared.security;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PermissionService {

    private static final String PLATFORM_OP_PREFIX = "com.sme.platform.";

    public boolean allow(Set<String> roles, String requiredPerm) {
        if (roles == null) return false;

        Set<String> rolesUpper = roles.stream()
                .map(r -> r != null ? r.trim().toUpperCase() : "")
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        // ADMIN (platform-level): only platform operations
        if (rolesUpper.contains("ADMIN")) {
            return requiredPerm != null && requiredPerm.startsWith(PLATFORM_OP_PREFIX);
        }

        // Block tenant roles from platform operations
        if (requiredPerm != null && requiredPerm.startsWith(PLATFORM_OP_PREFIX)) {
            return false;
        }

        // HR: highest tenant role, all com.sme.* operations
        if (rolesUpper.contains("HR")
                && requiredPerm != null && requiredPerm.startsWith("com.sme.")) {
            return true;
        }

        // MANAGER: all com.sme.* operations
        if (rolesUpper.contains("MANAGER")
                && requiredPerm != null && requiredPerm.startsWith("com.sme.")) {
            return true;
        }

        // STAFF: user list
        if ("com.sme.identity.user.list".equals(requiredPerm) && rolesUpper.contains("STAFF")) {
            return true;
        }

        // Survey response submit: any authenticated user
        if ("com.sme.survey.response.submit".equals(requiredPerm) && !rolesUpper.isEmpty()) {
            return true;
        }

        // IT: assignee-scoped onboarding tasks
        if (rolesUpper.contains("IT") && requiredPerm != null) {
            String perm = requiredPerm.trim();
            if ("com.sme.onboarding.task.listByAssignee".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.updateStatus".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.detail".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.schedule.propose".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.schedule.confirm".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.schedule.reschedule".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.schedule.cancel".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.schedule.markNoShow".equalsIgnoreCase(perm)) {
                return true;
            }
        }

        // EMPLOYEE: task page, onboarding view, notifications, documents, own profile
        if (rolesUpper.contains("EMPLOYEE") && requiredPerm != null) {
            String perm = requiredPerm.trim();
            if ("com.sme.onboarding.task.listByOnboarding".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.timelineByOnboarding".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.updateStatus".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.detail".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.acknowledge".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.schedule.propose".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.schedule.confirm".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.schedule.reschedule".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.schedule.cancel".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.schedule.markNoShow".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.attachment.add".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.approve".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.reject".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.instance.list".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.instance.get".equalsIgnoreCase(perm)
                    || "com.sme.notification.list".equalsIgnoreCase(perm)
                    || "com.sme.notification.markRead".equalsIgnoreCase(perm)
                    || "com.sme.content.document.list".equalsIgnoreCase(perm)
                    || "com.sme.content.document.acknowledge".equalsIgnoreCase(perm)
                    || "com.sme.identity.user.get".equalsIgnoreCase(perm)
                    || "com.sme.survey.instance.list".equalsIgnoreCase(perm)
                    || "com.sme.survey.question.list.bytemplate".equalsIgnoreCase(perm)
                    || "com.sme.survey.instance.get".equalsIgnoreCase(perm)
                    || "com.sme.survey.response.saveDraft".equalsIgnoreCase(perm)
                    || "com.sme.ai.assistant.ask".equalsIgnoreCase(perm)
                    || "com.sme.chat.session.create".equalsIgnoreCase(perm)
                    || "com.sme.chat.session.list".equalsIgnoreCase(perm)
                    || "com.sme.chat.message.list".equalsIgnoreCase(perm)) {
                return true;
            }
        }

        return false;
    }
}