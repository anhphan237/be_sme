package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.context.CompanySetupContext;
import com.sme.be_sme.modules.onboarding.api.request.CompanySetupRequest;
import com.sme.be_sme.modules.onboarding.api.response.CompanySetupResponse;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CompanySetupProcessor extends BaseCoreProcessor<CompanySetupContext> {

    private static final String ADMIN_ROLE_CODE = "ADMIN";

    private final ObjectMapper objectMapper;

    private final CompanySetupValidateCoreProcessor validate;
    private final CompanySetupCreateCompanyCoreProcessor createCompany;
    private final CompanySetupCreateDepartmentCoreProcessor createDepartment;
    private final CompanySetupCreateAdminUserCoreProcessor createAdminUser;
    private final CompanySetupUpsertAdminProfileCoreProcessor upsertProfile;
    private final CompanySetupAssignAdminRoleCoreProcessor assignRole;

    @Override
    protected CompanySetupContext buildContext(BizContext biz, JsonNode payload) {
        CompanySetupRequest req = objectMapper.convertValue(payload, CompanySetupRequest.class);

        CompanySetupContext ctx = new CompanySetupContext();
        ctx.setBiz(biz);
        ctx.setRequest(req);
        ctx.setResponse(new CompanySetupResponse());
        return ctx;
    }

    @Override
    @Transactional
    protected Object process(CompanySetupContext ctx) {
        validate.processWith(ctx);

        createCompany.processWith(ctx);
        createDepartment.processWith(ctx);
        createAdminUser.processWith(ctx);
        upsertProfile.processWith(ctx);
        assignRole.processWith(ctx);

        ctx.getResponse().setCompanyId(ctx.getCompany().getCompanyId());
        ctx.getResponse().setDepartmentId(ctx.getDepartment().getDepartmentId());
        ctx.getResponse().setAdminUserId(ctx.getAdminUser().getUserId());
        ctx.getResponse().setMemberUserId(null);
        return ctx.getResponse();
    }
}

