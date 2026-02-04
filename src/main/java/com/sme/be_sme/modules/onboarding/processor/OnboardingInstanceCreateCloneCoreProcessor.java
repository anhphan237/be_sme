package com.sme.be_sme.modules.onboarding.processor;

import com.sme.be_sme.modules.onboarding.context.OnboardingInstanceCreateContext;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.ChecklistInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.ChecklistInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.model.ChecklistTemplateRow;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.model.TaskTemplateRow;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OnboardingInstanceCreateCloneCoreProcessor
        extends BaseCoreProcessor<OnboardingInstanceCreateContext> {

    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final ChecklistInstanceMapper checklistInstanceMapper;
    private final TaskInstanceMapper taskInstanceMapper;

    @Override
    protected Object process(OnboardingInstanceCreateContext ctx) {

        String companyId = ctx.getBiz().getTenantId();
        String operatorId = ctx.getBiz().getOperatorId();

        // 1) onboarding_instances
        String onboardingId = UuidGenerator.generate();
        ctx.setInstanceId(onboardingId);

        OnboardingInstanceEntity inst = new OnboardingInstanceEntity();
        inst.setOnboardingId(onboardingId);
        inst.setCompanyId(companyId);
        inst.setEmployeeId(ctx.getRequest().getEmployeeId());
        inst.setOnboardingTemplateId(ctx.getTemplate().getOnboardingTemplateId());
        inst.setStatus("DRAFT");
        inst.setCreatedBy(operatorId);
        onboardingInstanceMapper.insert(inst);

        // 2) checklist_instances (tplChecklistId -> checklistId)
        Map<String, String> checklistMap = new HashMap<>();
        for (ChecklistTemplateRow r : ctx.getChecklistRows()) {
            String checklistId = UuidGenerator.generate();
            checklistMap.put(r.getChecklistTemplateId(), checklistId);

            ChecklistInstanceEntity chk = new ChecklistInstanceEntity();
            chk.setChecklistId(checklistId);
            chk.setCompanyId(companyId);
            chk.setOnboardingId(onboardingId);

            // ChecklistInstanceEntity KHÔNG có checklistTemplateId/orderNo/createdBy
            // Map tối thiểu để FE preview + runtime
            chk.setName(r.getName());
            chk.setStage(buildStage(r));          // String stage (tuỳ bạn)
            chk.setStatus("DRAFT");
            chk.setProgressPercent(0);

            checklistInstanceMapper.insert(chk);
        }

        // 3) task_instances (baseline) -> link checklist_instances via checklistId
        for (TaskTemplateRow r : ctx.getBaselineTaskRows()) {
            String taskId = UuidGenerator.generate();
            String checklistId = checklistMap.get(r.getChecklistTemplateId());

            TaskInstanceEntity t = new TaskInstanceEntity();
            t.setTaskId(taskId);
            t.setCompanyId(companyId);
            t.setChecklistId(checklistId);

            t.setTaskTemplateId(r.getTaskTemplateId());
            t.setTitle(r.getName());            // TaskInstanceEntity dùng title
            t.setDescription(r.getDescription());
            t.setStatus("DRAFT");
            t.setCreatedBy(operatorId);

            // dueDate/assigned... để null (draft)
            taskInstanceMapper.insert(t);
        }

        return null;
    }

    private String buildStage(ChecklistTemplateRow r) {
        // ChecklistInstanceEntity.stage là String; schema của bạn chưa cho thấy stage rule.
        // An toàn nhất: dùng orderNo nếu có, fallback name
        if (r.getOrderNo() != null) {
            return "STAGE_" + r.getOrderNo();
        }
        return r.getName();
    }
}
