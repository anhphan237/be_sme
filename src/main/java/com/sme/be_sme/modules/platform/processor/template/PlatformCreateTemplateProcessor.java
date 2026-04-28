package com.sme.be_sme.modules.platform.processor.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.platform.api.request.CreatePlatformTemplateRequest;
import com.sme.be_sme.modules.platform.context.PlatformCreateTemplateContext;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class PlatformCreateTemplateProcessor extends BaseCoreProcessor<PlatformCreateTemplateContext> {

    private final ObjectMapper objectMapper;
    private final PlatformCreateTemplateValidateCoreProcessor validate;
    private final PlatformCreateTemplateInsertTemplateCoreProcessor insertTemplate;
    private final PlatformCreateTemplateChecklistsAndTasksCoreProcessor createChecklistsAndTasks;
    private final PlatformCreateTemplateBuildResponseProcessor buildResponse;

    @Override
    protected PlatformCreateTemplateContext buildContext(BizContext context, JsonNode payload) {
        PlatformTemplateBizHelper.assertPlatformAdmin(context, "create");

        CreatePlatformTemplateRequest request =
                objectMapper.convertValue(payload, CreatePlatformTemplateRequest.class);

        PlatformCreateTemplateContext ctx = new PlatformCreateTemplateContext();
        ctx.setBiz(context);
        ctx.setRequest(request);
        ctx.setCompanyId(PlatformTemplateBizHelper.PLATFORM_COMPANY_ID);
        ctx.setTemplateId(UuidGenerator.generate());
        ctx.setNow(new Date());
        return ctx;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object process(PlatformCreateTemplateContext ctx) {
        validate.processWith(ctx);
        insertTemplate.processWith(ctx);
        createChecklistsAndTasks.processWith(ctx);
        buildResponse.processWith(ctx);
        return ctx.getResponse();
    }
}