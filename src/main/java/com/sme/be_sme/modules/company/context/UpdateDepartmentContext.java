package com.sme.be_sme.modules.company.context;

import com.sme.be_sme.modules.company.api.request.UpdateDepartmentRequest;
import com.sme.be_sme.modules.company.api.response.UpdateDepartmentResponse;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDepartmentContext {
    private BizContext biz;
    private UpdateDepartmentRequest request;
    private UpdateDepartmentResponse response;
}
