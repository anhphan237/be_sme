package com.sme.be_sme.modules.company.context;

import com.sme.be_sme.modules.company.api.request.CreateDepartmentRequest;
import com.sme.be_sme.modules.company.api.response.CreateDepartmentResponse;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDepartmentContext {
    private BizContext biz;
    private CreateDepartmentRequest request;
    private CreateDepartmentResponse response;

    private String departmentId;
}