package com.sme.be_sme.modules.platform.processor.template;

import com.sme.be_sme.modules.platform.context.PlatformCreateTemplateContext;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PlatformCreateTemplateValidateCoreProcessor extends BaseCoreProcessor<PlatformCreateTemplateContext> {

    @Override
    protected Object process(PlatformCreateTemplateContext ctx) {
        if (ctx.getRequest() == null || !StringUtils.hasText(ctx.getRequest().getName())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "name is required");
        }
        return null;
    }
}