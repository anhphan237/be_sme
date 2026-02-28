package com.sme.be_sme.modules.company.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.api.request.CompanyRegisterRequest;
import com.sme.be_sme.modules.company.api.response.CompanyRegisterResponse;
import com.sme.be_sme.modules.company.context.CompanyRegisterContext;
import com.sme.be_sme.modules.company.processor.registration.CompanyRegisterAssignAdminRoleCoreProcessor;
import com.sme.be_sme.modules.billing.service.CompanyRegistrationSubscriptionService;
import com.sme.be_sme.modules.company.processor.registration.CompanyRegisterCheckDupCoreProcessor;
import com.sme.be_sme.modules.company.processor.registration.CompanyRegisterCreateAdminUserCoreProcessor;
import com.sme.be_sme.modules.company.processor.registration.CompanyRegisterCreateCompanyCoreProcessor;
import com.sme.be_sme.modules.company.processor.registration.CompanyRegisterCreateDefaultRolesCoreProcessor;
import com.sme.be_sme.modules.company.processor.registration.CompanyRegisterValidateCoreProcessor;
import com.sme.be_sme.modules.identity.infrastructure.repository.UserRoleRepository;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.security.JwtProperties;
import com.sme.be_sme.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class CompanyRegisterProcessor extends BaseCoreProcessor<CompanyRegisterContext> {

    private final ObjectMapper objectMapper;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserRoleRepository userRoleRepository;

    private final CompanyRegisterValidateCoreProcessor validate;
    private final CompanyRegisterCheckDupCoreProcessor checkDup;
    private final CompanyRegisterCreateCompanyCoreProcessor createCompany;
    private final CompanyRegisterCreateAdminUserCoreProcessor createAdminUser;
    private final CompanyRegisterCreateDefaultRolesCoreProcessor createDefaultRoles;
    private final CompanyRegisterSeedRolePermissionsCoreProcessor seedRolePermissions;
    private final CompanyRegisterAssignAdminRoleCoreProcessor assignAdminRole;
    private final CompanyRegistrationSubscriptionService subscriptionService;

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
        subscriptionService.createSubscriptionForCompany(ctx.getCompanyId(), ctx.getRequest().getPlanCode());
        createAdminUser.processWith(ctx);
        createDefaultRoles.processWith(ctx);
        seedRolePermissions.processWith(ctx);
        assignAdminRole.processWith(ctx);

        String companyId = ctx.getCompany().getCompanyId();
        String adminUserId = ctx.getAdminUser().getUserId();

        ctx.getResponse().setCompanyId(companyId);
        ctx.getResponse().setAdminUserId(adminUserId);

        Set<String> roles = userRoleRepository.findRoles(companyId, adminUserId);
        String token = jwtService.issueAccessToken(adminUserId, companyId, roles);
        ctx.getResponse().setAccessToken(token);
        ctx.getResponse().setTokenType("Bearer");
        ctx.getResponse().setExpiresInSeconds(jwtProperties.getAccessTtlSeconds());

        return ctx.getResponse();
    }
}
