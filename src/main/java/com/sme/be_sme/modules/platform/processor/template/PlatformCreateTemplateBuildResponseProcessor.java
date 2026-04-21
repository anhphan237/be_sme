package com.sme.be_sme.modules.platform.processor.template;

import com.sme.be_sme.modules.platform.api.response.CreatePlatformTemplateResponse;
import com.sme.be_sme.modules.platform.context.PlatformCreateTemplateContext;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import org.springframework.stereotype.Component;

@Component
public class PlatformCreateTemplateBuildResponseProcessor extends BaseCoreProcessor<PlatformCreateTemplateContext> {

    @Override
    protected Object process(PlatformCreateTemplateContext ctx) {
        CreatePlatformTemplateResponse response = new CreatePlatformTemplateResponse();
        response.setTemplateId(ctx.getTemplateId());
        response.setName(ctx.getTemplateEntity().getName());
        response.setStatus(ctx.getTemplateEntity().getStatus());
        response.setTemplateKind(ctx.getTemplateEntity().getTemplateKind());
        response.setDepartmentTypeCode(ctx.getTemplateEntity().getDepartmentTypeCode());
        response.setLevel(ctx.getTemplateEntity().getLevel());
        ctx.setResponse(response);
        return null;
    }
}
