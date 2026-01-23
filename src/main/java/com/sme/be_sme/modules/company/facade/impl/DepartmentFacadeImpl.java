package com.sme.be_sme.modules.company.facade.impl;

import com.sme.be_sme.modules.company.api.request.CreateDepartmentRequest;
import com.sme.be_sme.modules.company.api.response.CreateDepartmentResponse;
import com.sme.be_sme.modules.company.facade.DepartmentFacade;
import com.sme.be_sme.modules.company.processor.CreateDepartmentProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import com.sme.be_sme.shared.gateway.core.BizContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DepartmentFacadeImpl extends BaseOperationFacade implements DepartmentFacade {

    private final CreateDepartmentProcessor createDepartmentProcessor;

    @Override
    public CreateDepartmentResponse createDepartment(CreateDepartmentRequest request) {
        return call(createDepartmentProcessor, request, CreateDepartmentResponse.class);
    }
}
