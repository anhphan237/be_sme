package com.sme.be_sme.modules.analytics.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.analytics.api.request.CompanyOnboardingTemplateScoreboardRequest;
import com.sme.be_sme.modules.analytics.api.response.CompanyOnboardingTemplateScoreboardResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.service.OnboardingInstanceProgressService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class CompanyOnboardingTemplateScoreboardProcessor extends BaseBizProcessor<BizContext> {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 200;

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final TaskInstanceMapper taskInstanceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        CompanyOnboardingTemplateScoreboardRequest request =
                objectMapper.convertValue(payload, CompanyOnboardingTemplateScoreboardRequest.class);
        validate(context, request);

        String companyId = resolveCompanyId(context, request);
        String templateId = request.getTemplateId().trim();
        String status = normalizeStatus(request.getStatus());
        int limit = normalizeLimit(request.getLimit());

        Date now = new Date();
        List<CompanyOnboardingTemplateScoreboardResponse.CandidateScoreItem> allCandidates = new ArrayList<>();
        for (OnboardingInstanceEntity instance : onboardingInstanceMapper.selectAll()) {
            if (instance == null || !companyId.equals(instance.getCompanyId())) {
                continue;
            }
            if (!templateId.equals(normalize(instance.getOnboardingTemplateId()))) {
                continue;
            }
            if (status != null && !status.equals(normalize(instance.getStatus()))) {
                continue;
            }
            allCandidates.add(toCandidate(instance, now));
        }

        allCandidates.sort((a, b) -> {
            int c1 = compareDoubleDesc(a.getQualityScore(), b.getQualityScore());
            if (c1 != 0) {
                return c1;
            }
            int c2 = compareIntegerDesc(a.getProgressPercent(), b.getProgressPercent());
            if (c2 != 0) {
                return c2;
            }
            int c3 = compareDoubleDesc(a.getCompletionRate(), b.getCompletionRate());
            if (c3 != 0) {
                return c3;
            }
            return compareIntegerDesc(a.getCompletedTasks(), b.getCompletedTasks());
        });

        for (int i = 0; i < allCandidates.size(); i++) {
            allCandidates.get(i).setRank(i + 1);
        }

        List<CompanyOnboardingTemplateScoreboardResponse.CandidateScoreItem> selected =
                allCandidates.size() > limit ? allCandidates.subList(0, limit) : allCandidates;

        CompanyOnboardingTemplateScoreboardResponse response = new CompanyOnboardingTemplateScoreboardResponse();
        response.setCompanyId(companyId);
        response.setTemplateId(templateId);
        response.setStatus(status);
        response.setTotalCandidates(allCandidates.size());
        response.setCandidates(selected);
        return response;
    }

    private CompanyOnboardingTemplateScoreboardResponse.CandidateScoreItem toCandidate(
            OnboardingInstanceEntity instance,
            Date now) {
        List<TaskInstanceEntity> tasks = taskInstanceMapper.selectByCompanyIdAndOnboardingId(
                instance.getCompanyId(),
                instance.getOnboardingId());
        int totalTasks = tasks == null ? 0 : tasks.size();
        int completedTasks = 0;
        int overdueTasks = 0;
        int lateCompletedTasks = 0;
        if (tasks != null) {
            for (TaskInstanceEntity task : tasks) {
                if (task == null) {
                    continue;
                }
                boolean completed = OnboardingInstanceProgressService.isEffectivelyComplete(task);
                if (completed) {
                    completedTasks++;
                }
                if (task.getDueDate() != null && !completed && task.getDueDate().before(now)) {
                    overdueTasks++;
                }
                if (completed
                        && task.getDueDate() != null
                        && task.getCompletedAt() != null
                        && task.getCompletedAt().after(task.getDueDate())) {
                    lateCompletedTasks++;
                }
            }
        }

        double completionRate = totalTasks == 0 ? 0d : (completedTasks * 100d) / totalTasks;
        double qualityScore = clampScore(completionRate - (overdueTasks * 10d) - (lateCompletedTasks * 5d));

        CompanyOnboardingTemplateScoreboardResponse.CandidateScoreItem item =
                new CompanyOnboardingTemplateScoreboardResponse.CandidateScoreItem();
        item.setInstanceId(instance.getOnboardingId());
        item.setEmployeeId(instance.getEmployeeId());
        item.setProgressPercent(instance.getProgressPercent() != null ? instance.getProgressPercent() : 0);
        item.setTotalTasks(totalTasks);
        item.setCompletedTasks(completedTasks);
        item.setOverdueTasks(overdueTasks);
        item.setLateCompletedTasks(lateCompletedTasks);
        item.setCompletionRate(round2(completionRate));
        item.setQualityScore(round2(qualityScore));
        return item;
    }

    private static String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "ACTIVE";
        }
        return normalize(status);
    }

    private static int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static void validate(BizContext context, CompanyOnboardingTemplateScoreboardRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getTemplateId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "templateId is required");
        }
        if (request.getLimit() != null && request.getLimit() <= 0) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "limit must be greater than 0");
        }
        if (StringUtils.hasText(request.getCompanyId())
                && !Objects.equals(request.getCompanyId().trim(), context.getTenantId().trim())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "companyId does not match tenant");
        }
    }

    private static String resolveCompanyId(BizContext context, CompanyOnboardingTemplateScoreboardRequest request) {
        if (StringUtils.hasText(request.getCompanyId())) {
            return request.getCompanyId().trim();
        }
        return context.getTenantId().trim();
    }

    private static String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static int compareDoubleDesc(Double a, Double b) {
        return Double.compare(b == null ? 0d : b, a == null ? 0d : a);
    }

    private static int compareIntegerDesc(Integer a, Integer b) {
        return Integer.compare(b == null ? 0 : b, a == null ? 0 : a);
    }

    private static double clampScore(double score) {
        if (score < 0d) {
            return 0d;
        }
        if (score > 100d) {
            return 100d;
        }
        return score;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
