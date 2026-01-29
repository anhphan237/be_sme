package com.sme.be_sme.modules.company.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.api.request.CompanyRegisterRequest;
import com.sme.be_sme.modules.company.api.response.CompanyRegisterResponse;
import com.sme.be_sme.modules.company.context.CompanyRegisterContext;
import com.sme.be_sme.modules.company.processor.registration.CompanyRegisterAssignAdminRoleCoreProcessor;
import com.sme.be_sme.modules.company.processor.registration.CompanyRegisterCheckDupCoreProcessor;
import com.sme.be_sme.modules.company.processor.registration.CompanyRegisterCreateAdminUserCoreProcessor;
import com.sme.be_sme.modules.company.processor.registration.CompanyRegisterCreateCompanyCoreProcessor;
import com.sme.be_sme.modules.company.processor.registration.CompanyRegisterCreateDefaultRolesCoreProcessor;
import com.sme.be_sme.modules.company.processor.registration.CompanyRegisterValidateCoreProcessor;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CompanyRegisterProcessor extends BaseCoreProcessor<CompanyRegisterContext> {

    private final ObjectMapper objectMapper;

    private final CompanyRegisterValidateCoreProcessor validate;
    private final CompanyRegisterCheckDupCoreProcessor checkDup;
    private final CompanyRegisterCreateCompanyCoreProcessor createCompany;
    private final CompanyRegisterCreateAdminUserCoreProcessor createAdminUser;
    private final CompanyRegisterCreateDefaultRolesCoreProcessor createDefaultRoles;
    private final CompanyRegisterSeedRolePermissionsCoreProcessor seedRolePermissions;
    private final CompanyRegisterAssignAdminRoleCoreProcessor assignAdminRole;

    @Override
    protected CompanyRegisterContext buildContext(BizContext biz, JsonNode payload) {
        CompanyRegisterRequest req = objectMapper.convertValue(payload, CompanyRegisterRequest.class);

        CompanyRegisterContext ctx = new CompanyRegisterContext();
        ctx.setBiz(biz);
        ctx.setRequest(req);
        ctx.setResponse(new CompanyRegisterResponse());
        return ctx;
    }

    @Override
    @Transactional
    protected Object process(CompanyRegisterContext ctx) {
        validate.processWith(ctx);
        checkDup.processWith(ctx);
        createCompany.processWith(ctx);
        createAdminUser.processWith(ctx);
        createDefaultRoles.processWith(ctx);
        seedRolePermissions.processWith(ctx);
        assignAdminRole.processWith(ctx);

        ctx.getResponse().setCompanyId(ctx.getCompany().getCompanyId());
        ctx.getResponse().setAdminUserId(ctx.getAdminUser().getUserId());
        return ctx.getResponse();
    }
}
