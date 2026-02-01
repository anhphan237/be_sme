package com.sme.be_sme.modules.company.facade.impl;

import com.sme.be_sme.modules.company.api.request.CreateDepartmentRequest;
import com.sme.be_sme.modules.company.api.request.ListDepartmentRequest;
import com.sme.be_sme.modules.company.api.request.UpdateDepartmentRequest;
import com.sme.be_sme.modules.company.api.response.CreateDepartmentResponse;
import com.sme.be_sme.modules.company.api.response.ListDepartmentResponse;
import com.sme.be_sme.modules.company.api.response.UpdateDepartmentResponse;
import com.sme.be_sme.modules.company.facade.DepartmentFacade;
import com.sme.be_sme.modules.company.processor.CreateDepartmentProcessor;
import com.sme.be_sme.modules.company.processor.ListDepartmentProcessor;
import com.sme.be_sme.modules.company.processor.UpdateDepartmentProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import com.sme.be_sme.shared.gateway.core.BizContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DepartmentFacadeImpl extends BaseOperationFacade implements DepartmentFacade {

    private final CreateDepartmentProcessor createDepartmentProcessor;
    private final UpdateDepartmentProcessor updateDepartmentProcessor;
    private final ListDepartmentProcessor listDepartmentProcessor;

    @Override
    public CreateDepartmentResponse createDepartment(CreateDepartmentRequest request) {
        return call(createDepartmentProcessor, request, CreateDepartmentResponse.class);
    }

    @Override
    public UpdateDepartmentResponse updateDepartment(UpdateDepartmentRequest request) {
        return call(updateDepartmentProcessor, request, UpdateDepartmentResponse.class);
    }

    @Override
    public ListDepartmentResponse listDepartments(ListDepartmentRequest request) {
        return call(listDepartmentProcessor, request, ListDepartmentResponse.class);
    }

    @Override
    public CreateDepartmentResponse createOrgDepartment(CreateDepartmentRequest request) {
        return call(createDepartmentProcessor, request, CreateDepartmentResponse.class);
    }

    @Override
    public UpdateDepartmentResponse updateOrgDepartment(UpdateDepartmentRequest request) {
        return call(updateDepartmentProcessor, request, UpdateDepartmentResponse.class);
    }
}
