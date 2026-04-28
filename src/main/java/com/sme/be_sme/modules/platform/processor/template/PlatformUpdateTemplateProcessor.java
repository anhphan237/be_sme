package com.sme.be_sme.modules.platform.processor.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
import com.sme.be_sme.modules.platform.api.request.UpdatePlatformTemplateRequest;
import com.sme.be_sme.modules.platform.api.response.CreatePlatformTemplateResponse;
import com.sme.be_sme.modules.platform.context.PlatformCreateTemplateContext;
import com.sme.be_sme.modules.platform.infrastructure.mapper.PlatformTemplateMapperExt;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class PlatformUpdateTemplateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingTemplateMapper onboardingTemplateMapper;
    private final PlatformTemplateMapperExt platformTemplateMapperExt;
    private final PlatformCreateTemplateChecklistsAndTasksCoreProcessor createChecklistsAndTasks;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformTemplateBizHelper.assertPlatformAdmin(context, "update");

        UpdatePlatformTemplateRequest request =
                objectMapper.convertValue(payload, UpdatePlatformTemplateRequest.class);

        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "request is required");
        }

        String templateId = PlatformTemplateBizHelper.requireTemplateId(request.getTemplateId());

        if (!StringUtils.hasText(request.getName())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "name is required");
        }

        OnboardingTemplateEntity entity =
                PlatformTemplateBizHelper.getPlatformTemplate(onboardingTemplateMapper, templateId);

        int usageCount = platformTemplateMapperExt.countTemplateUsage(templateId);
        boolean requestStructureUpdate = request.getChecklists() != null;

        if (usageCount > 0 && requestStructureUpdate) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "template already used; structure cannot be changed, archive or create a new version instead");
        }

        Date now = new Date();

        entity.setName(request.getName().trim());
        entity.setDescription(request.getDescription());
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus().trim().toUpperCase(Locale.US) : entity.getStatus());
        entity.setTemplateKind(
                StringUtils.hasText(request.getTemplateKind())
                        ? request.getTemplateKind().trim().toUpperCase(Locale.US)
                        : "ONBOARDING");
        entity.setDepartmentTypeCode(
                StringUtils.hasText(request.getDepartmentTypeCode())
                        ? request.getDepartmentTypeCode().trim().toUpperCase(Locale.US)
                        : null);
        entity.setUpdatedAt(now);

        if (onboardingTemplateMapper.updateByPrimaryKey(entity) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "update platform template failed");
        }

        if (requestStructureUpdate) {
            platformTemplateMapperExt.deleteRequiredDocumentsByTemplateId(
                    PlatformTemplateBizHelper.PLATFORM_COMPANY_ID,
                    templateId
            );
            platformTemplateMapperExt.deleteTasksByTemplateId(
                    PlatformTemplateBizHelper.PLATFORM_COMPANY_ID,
                    templateId
            );
            platformTemplateMapperExt.deleteChecklistsByTemplateId(
                    PlatformTemplateBizHelper.PLATFORM_COMPANY_ID,
                    templateId
            );

            if (!CollectionUtils.isEmpty(request.getChecklists())) {
                PlatformCreateTemplateContext createCtx = new PlatformCreateTemplateContext();
                createCtx.setBiz(context);
                createCtx.setRequest(request);
                createCtx.setCompanyId(PlatformTemplateBizHelper.PLATFORM_COMPANY_ID);
                createCtx.setTemplateId(templateId);
                createCtx.setTemplateEntity(entity);
                createCtx.setNow(now);

                createChecklistsAndTasks.processWith(createCtx);
            }
        }

        return toResponse(entity);
    }

    private static CreatePlatformTemplateResponse toResponse(OnboardingTemplateEntity entity) {
        CreatePlatformTemplateResponse response = new CreatePlatformTemplateResponse();
        response.setTemplateId(entity.getOnboardingTemplateId());
        response.setName(entity.getName());
        response.setStatus(entity.getStatus());
        response.setTemplateKind(entity.getTemplateKind());
        response.setDepartmentTypeCode(entity.getDepartmentTypeCode());
        response.setLevel(entity.getLevel());
        return response;
    }
}