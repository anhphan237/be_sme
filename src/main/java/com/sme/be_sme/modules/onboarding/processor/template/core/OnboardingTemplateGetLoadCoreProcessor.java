package com.sme.be_sme.modules.onboarding.processor.template.core;

import com.sme.be_sme.modules.onboarding.context.OnboardingTemplateGetContext;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingTemplateMapperExt;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskTemplateDepartmentCheckpointMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskTemplateRequiredDocumentMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskTemplateDepartmentCheckpointEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskTemplateRequiredDocumentEntity;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingTemplateGetLoadCoreProcessor extends BaseCoreProcessor<OnboardingTemplateGetContext> {

    private final OnboardingTemplateMapperExt onboardingTemplateMapperExt;
    private final TaskTemplateRequiredDocumentMapper taskTemplateRequiredDocumentMapper;
    private final TaskTemplateDepartmentCheckpointMapper taskTemplateDepartmentCheckpointMapper;

    @Override
    protected Object process(OnboardingTemplateGetContext ctx) {
        String companyId = ctx.getBiz().getTenantId();
        String templateId = ctx.getRequest().getTemplateId();

        OnboardingTemplateEntity template = onboardingTemplateMapperExt.selectTemplateByIdAndCompany(templateId, companyId);
        if (template == null) {
            throw AppException.of("TEMPLATE_NOT_FOUND", "template not found");
        }

        String templateCompanyId = template.getCompanyId();
        ctx.setTemplate(template);
        ctx.setChecklistRows(onboardingTemplateMapperExt.selectChecklistRows(templateCompanyId, templateId));
        ctx.setBaselineTaskRows(onboardingTemplateMapperExt.selectBaselineTaskRows(templateCompanyId, templateId));
        List<String> taskTemplateIds = ctx.getBaselineTaskRows() == null ? List.of()
                : ctx.getBaselineTaskRows().stream()
                .map(r -> r.getTaskTemplateId())
                .filter(id -> id != null && !id.isBlank())
                .toList();
        if (taskTemplateIds.isEmpty()) {
            ctx.setRequiredDocumentIdsByTaskTemplateId(Collections.emptyMap());
            ctx.setResponsibleDepartmentIdsByTaskTemplateId(Collections.emptyMap());
        } else {
            Map<String, List<String>> requiredDocsByTaskTemplateId =
                    taskTemplateRequiredDocumentMapper
                            .selectByCompanyIdAndTaskTemplateIds(templateCompanyId, taskTemplateIds)
                            .stream()
                            .collect(Collectors.groupingBy(
                                    TaskTemplateRequiredDocumentEntity::getTaskTemplateId,
                                    Collectors.mapping(TaskTemplateRequiredDocumentEntity::getDocumentId, Collectors.toList())
                            ));
            ctx.setRequiredDocumentIdsByTaskTemplateId(requiredDocsByTaskTemplateId);
            Map<String, List<String>> responsibleDepartmentIdsByTaskTemplateId =
                    taskTemplateDepartmentCheckpointMapper
                            .selectByCompanyIdAndTaskTemplateIds(templateCompanyId, taskTemplateIds)
                            .stream()
                            .collect(Collectors.groupingBy(
                                    TaskTemplateDepartmentCheckpointEntity::getTaskTemplateId,
                                    Collectors.mapping(TaskTemplateDepartmentCheckpointEntity::getDepartmentId, Collectors.toList())
                            ));
            ctx.setResponsibleDepartmentIdsByTaskTemplateId(responsibleDepartmentIdsByTaskTemplateId);
        }
        return null;
    }
}
