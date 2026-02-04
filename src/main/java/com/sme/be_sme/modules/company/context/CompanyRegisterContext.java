package com.sme.be_sme.modules.company.context;

import com.sme.be_sme.modules.company.api.request.CompanyRegisterRequest;
import com.sme.be_sme.modules.company.api.response.CompanyRegisterResponse;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.RoleEntity;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CompanyRegisterContext {
    private BizContext biz;
    private CompanyRegisterRequest request;
    private CompanyRegisterResponse response;

    private CompanyEntity company;
    private UserEntity adminUser;
    private List<RoleEntity> defaultRoles;

    private String companyId;
    private String adminUserId;
}
