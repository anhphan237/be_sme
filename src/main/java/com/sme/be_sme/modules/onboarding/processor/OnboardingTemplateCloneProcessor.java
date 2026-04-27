package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateCloneRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateCreateRequest;
import com.sme.be_sme.modules.onboarding.context.OnboardingTemplateCreateContext;
import com.sme.be_sme.modules.onboarding.processor.template.core.OnboardingTemplateCreateBuildResponseCoreProcessor;
import com.sme.be_sme.modules.onboarding.processor.template.core.OnboardingTemplateCreateChecklistsAndTasksCoreProcessor;
import com.sme.be_sme.modules.onboarding.processor.template.core.OnboardingTemplateCreateCloneSourceCoreProcessor;
import com.sme.be_sme.modules.onboarding.processor.template.core.OnboardingTemplateCreateInsertTemplateCoreProcessor;
import com.sme.be_sme.modules.onboarding.processor.template.core.OnboardingTemplateCreateValidateCoreProcessor;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OnboardingTemplateCloneProcessor extends BaseCoreProcessor<OnboardingTemplateCreateContext> {

    private static final String LEVEL_PLATFORM = "PLATFORM";
    private static final String LEVEL_TENANT = "TENANT";

    private final ObjectMapper objectMapper;
    private final OnboardingTemplateCreateValidateCoreProcessor validate;
    private final OnboardingTemplateCreateCloneSourceCoreProcessor cloneSource;
    private final OnboardingTemplateCreateInsertTemplateCoreProcessor insertTemplate;
    private final OnboardingTemplateCreateChecklistsAndTasksCoreProcessor createChecklistsAndTasks;
    private final OnboardingTemplateCreateBuildResponseCoreProcessor buildResponse;

    @Override
    protected OnboardingTemplateCreateContext buildContext(BizContext biz, JsonNode payload) {
        OnboardingTemplateCloneRequest request = objectMapper.convertValue(payload, OnboardingTemplateCloneRequest.class);
        validateCloneRequest(request);

        OnboardingTemplateCreateRequest createRequest = new OnboardingTemplateCreateRequest();
        createRequest.setSourceTemplateId(request.getSourceTemplateId().trim());
        createRequest.setSourceTemplateLevel(request.getLevel().trim().toUpperCase());
        createRequest.setName(request.getName().trim());
        createRequest.setDescription(request.getDescription());
        createRequest.setStatus(request.getStatus());
        createRequest.setCreatedBy(request.getCreatedBy());

        OnboardingTemplateCreateContext ctx = new OnboardingTemplateCreateContext();
        ctx.setBiz(biz);
        ctx.setRequest(createRequest);
        ctx.setCompanyId(biz.getTenantId());
        ctx.setTemplateId(UuidGenerator.generate());
        ctx.setNow(new Date());
        return ctx;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object process(OnboardingTemplateCreateContext ctx) {
        validate.processWith(ctx);
        cloneSource.processWith(ctx);
        insertTemplate.processWith(ctx);
        createChecklistsAndTasks.processWith(ctx);
        buildResponse.processWith(ctx);
        return ctx.getResponse();
    }

    private static void validateCloneRequest(OnboardingTemplateCloneRequest request) {
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getSourceTemplateId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "sourceTemplateId is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "name is required");
        }
        if (!StringUtils.hasText(request.getLevel())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "level is required");
        }
        String level = request.getLevel().trim();
        if (!LEVEL_PLATFORM.equalsIgnoreCase(level) && !LEVEL_TENANT.equalsIgnoreCase(level)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "level must be PLATFORM or TENANT");
        }
    }
}
