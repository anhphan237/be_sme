package com.sme.be_sme.modules.onboarding.service;

import com.sme.be_sme.modules.onboarding.infrastructure.mapper.ChecklistInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskWorkflow;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OnboardingInstanceProgressService {

    private final TaskInstanceMapper taskInstanceMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final ChecklistInstanceMapper checklistInstanceMapper;

    public static boolean isEffectivelyComplete(TaskInstanceEntity t) {
        if (t == null || !"DONE".equalsIgnoreCase(t.getStatus())) {
            return false;
        }
        if (Boolean.TRUE.equals(t.getRequiresManagerApproval())) {
            return OnboardingTaskWorkflow.APPROVAL_APPROVED.equalsIgnoreCase(
                    t.getApprovalStatus() == null ? "" : t.getApprovalStatus());
        }
        return true;
    }

    public void recalculateFromTask(String companyId, TaskInstanceEntity task) {
        if (task == null || !StringUtils.hasText(task.getChecklistId())) {
            return;
        }
        var checklist = checklistInstanceMapper.selectByPrimaryKey(task.getChecklistId());
        if (checklist == null || !companyId.equals(checklist.getCompanyId())) {
            return;
        }
        recalculate(companyId, checklist.getOnboardingId());
    }

    public void recalculate(String companyId, String onboardingId) {
        if (!StringUtils.hasText(onboardingId)) {
            return;
        }
        OnboardingInstanceEntity instance = onboardingInstanceMapper.selectByPrimaryKey(onboardingId);
        if (instance == null || !companyId.equals(instance.getCompanyId())) {
            return;
        }
        List<TaskInstanceEntity> tasks = taskInstanceMapper.selectByCompanyIdAndOnboardingId(companyId, onboardingId);
        int total = tasks == null ? 0 : tasks.size();
        if (total == 0) {
            return;
        }
        long doneCount = tasks.stream().filter(OnboardingInstanceProgressService::isEffectivelyComplete).count();
        int progressPercent = (int) ((doneCount * 100) / total);
        instance.setProgressPercent(progressPercent);
        instance.setUpdatedAt(new Date());
        onboardingInstanceMapper.updateByPrimaryKey(instance);
    }
}
