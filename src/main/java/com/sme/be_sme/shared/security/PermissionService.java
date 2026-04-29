package com.sme.be_sme.shared.security;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
    public class PermissionService {

    /** Platform-only permissions: not granted to company HR. */
    private static final String PLATFORM_PREFIX = "com.sme.analytics.platform.";

    public boolean allow(Set<String> roles, String requiredPerm) {
        if (roles == null) return false;

        Set<String> rolesUpper = roles.stream()
                .map(r -> r != null ? r.trim().toUpperCase() : "")
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        if (rolesUpper.contains("ADMIN")) return true;

        // Platform-only: reserved for platform admin (e.g. subscription metrics)
        if (requiredPerm != null && requiredPerm.startsWith(PLATFORM_PREFIX)) {
            return false;
        }

        // HR / MANAGER: almost all company (tenant) permissions
        if ((rolesUpper.contains("HR") || rolesUpper.contains("HR_ADMIN") || rolesUpper.contains("MANAGER"))
                && requiredPerm != null && requiredPerm.startsWith("com.sme.")) {
            return true;
        }

        // Document editor / collaboration: any authenticated tenant user may invoke gateway operations;
        // fine-grained access per document is enforced in processors (e.g. DocumentAccessEvaluator).
        if (requiredPerm != null && requiredPerm.startsWith("com.sme.document.") && !rolesUpper.isEmpty()) {
            return true;
        }

        // Onboarding task comments: any authenticated tenant user may invoke comment operations;
        // fine-grained access per task/comment is enforced in processors.
        if (requiredPerm != null && requiredPerm.startsWith("com.sme.onboarding.task.comment.") && !rolesUpper.isEmpty()) {
            return true;
        }

        // user.list: STAFF (platform) can view list
        if ("com.sme.identity.user.list".equals(requiredPerm) && rolesUpper.contains("STAFF")) {
            return true;
        }

        // survey response submit: any authenticated user (e.g. employee filling survey)
        if ("com.sme.survey.response.submit".equals(requiredPerm) && !rolesUpper.isEmpty()) {
            return true;
        }

        // EMPLOYEE: task page, onboarding view, notifications, documents, own profile
        if (rolesUpper.contains("EMPLOYEE") && requiredPerm != null) {
            String perm = requiredPerm.trim();
            if ("com.sme.onboarding.task.listByOnboarding".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.listByAssignee".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.updateStatus".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.detail".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.comment.add".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.comment.list".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.comment.tree".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.acknowledge".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.attachment.add".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.schedule.list".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.approve".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.task.reject".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.event.list".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.instance.list".equalsIgnoreCase(perm)
                    || "com.sme.onboarding.instance.get".equalsIgnoreCase(perm)
                    || "com.sme.notification.list".equalsIgnoreCase(perm)
                    || "com.sme.notification.markRead".equalsIgnoreCase(perm)
                    || "com.sme.content.document.upload".equalsIgnoreCase(perm)
                    || "com.sme.content.document.list".equalsIgnoreCase(perm)
                    || "com.sme.content.document.acknowledge".equalsIgnoreCase(perm)
                    || "com.sme.document.detail".equalsIgnoreCase(perm)
                    || "com.sme.document.list".equalsIgnoreCase(perm)
                    || "com.sme.document.block.list".equalsIgnoreCase(perm)
                    || "com.sme.document.block.create".equalsIgnoreCase(perm)
                    || "com.sme.document.block.update".equalsIgnoreCase(perm)
                    || "com.sme.document.block.move".equalsIgnoreCase(perm)
                    || "com.sme.document.block.delete".equalsIgnoreCase(perm)
                    || "com.sme.document.version.list".equalsIgnoreCase(perm)
                    || "com.sme.document.version.get".equalsIgnoreCase(perm)
                    || "com.sme.document.version.compare".equalsIgnoreCase(perm)
                    || "com.sme.document.read.mark".equalsIgnoreCase(perm)
                    || "com.sme.document.read.list".equalsIgnoreCase(perm)
                    || "com.sme.document.folder.list".equalsIgnoreCase(perm)
                    || "com.sme.document.folder.tree".equalsIgnoreCase(perm)
                    || "com.sme.document.folder.delete".equalsIgnoreCase(perm)
                    || "com.sme.document.comment.list".equalsIgnoreCase(perm)
                    || "com.sme.document.comment.tree".equalsIgnoreCase(perm)
                    || "com.sme.document.comment.add".equalsIgnoreCase(perm)
                    || "com.sme.document.comment.delete".equalsIgnoreCase(perm)
                    || "com.sme.document.comment.update".equalsIgnoreCase(perm)
                    || "com.sme.document.accessRule.list".equalsIgnoreCase(perm)
                    || "com.sme.document.link.list".equalsIgnoreCase(perm)
                    || "com.sme.document.assignment.list".equalsIgnoreCase(perm)
                    || "com.sme.document.attachment.list".equalsIgnoreCase(perm)
                    || "com.sme.identity.user.get".equalsIgnoreCase(perm)
                    || "com.sme.survey.instance.list".equalsIgnoreCase(perm)
                    || "com.sme.survey.question.list.bytemplate".equalsIgnoreCase(perm)
                    || "com.sme.survey.instance.get".equalsIgnoreCase(perm)
                    || "com.sme.survey.response.saveDraft".equalsIgnoreCase(perm)
                    || "com.sme.ai.assistant.ask".equalsIgnoreCase(perm)
                    || "com.sme.chat.session.create".equalsIgnoreCase(perm)
                    || "com.sme.chat.session.list".equalsIgnoreCase(perm)
                    || "com.sme.chat.message.list".equalsIgnoreCase(perm)){
                return true;
            }
        }

        return false;
    }
}