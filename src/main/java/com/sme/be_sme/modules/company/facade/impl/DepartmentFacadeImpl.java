package com.sme.be_sme.modules.company.facade.impl;

import com.sme.be_sme.modules.company.api.request.CreateDepartmentRequest;
import com.sme.be_sme.modules.company.api.request.CreateDepartmentTypeRequest;
import com.sme.be_sme.modules.company.api.request.DeleteDepartmentTypeRequest;
import com.sme.be_sme.modules.company.api.request.ListDepartmentRequest;
import com.sme.be_sme.modules.company.api.request.ListDepartmentTypeRequest;
import com.sme.be_sme.modules.company.api.request.UpdateDepartmentRequest;
import com.sme.be_sme.modules.company.api.request.UpdateDepartmentTypeRequest;
import com.sme.be_sme.modules.company.api.response.CreateDepartmentResponse;
import com.sme.be_sme.modules.company.api.response.CreateDepartmentTypeResponse;
import com.sme.be_sme.modules.company.api.response.DeleteDepartmentTypeResponse;
import com.sme.be_sme.modules.company.api.response.ListDepartmentResponse;
import com.sme.be_sme.modules.company.api.response.ListDepartmentTypeResponse;
import com.sme.be_sme.modules.company.api.response.UpdateDepartmentResponse;
import com.sme.be_sme.modules.company.api.response.UpdateDepartmentTypeResponse;
import com.sme.be_sme.modules.company.facade.DepartmentFacade;
import com.sme.be_sme.modules.company.processor.CreateDepartmentProcessor;
import com.sme.be_sme.modules.company.processor.DepartmentTypeCreateProcessor;
import com.sme.be_sme.modules.company.processor.DepartmentTypeDeleteProcessor;
import com.sme.be_sme.modules.company.processor.DepartmentTypeListProcessor;
import com.sme.be_sme.modules.company.processor.DepartmentTypeUpdateProcessor;
import com.sme.be_sme.modules.company.processor.ListDepartmentProcessor;
import com.sme.be_sme.modules.company.processor.UpdateDepartmentProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DepartmentFacadeImpl extends BaseOperationFacade implements DepartmentFacade {

    private final CreateDepartmentProcessor createDepartmentProcessor;
    private final UpdateDepartmentProcessor updateDepartmentProcessor;
    private final ListDepartmentProcessor listDepartmentProcessor;
    private final DepartmentTypeCreateProcessor departmentTypeCreateProcessor;
    private final DepartmentTypeListProcessor departmentTypeListProcessor;
    private final DepartmentTypeUpdateProcessor departmentTypeUpdateProcessor;
    private final DepartmentTypeDeleteProcessor departmentTypeDeleteProcessor;

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

    @Override
    public CreateDepartmentTypeResponse createDepartmentType(CreateDepartmentTypeRequest request) {
        return call(departmentTypeCreateProcessor, request, CreateDepartmentTypeResponse.class);
    }

    @Override
    public ListDepartmentTypeResponse listDepartmentTypes(ListDepartmentTypeRequest request) {
        return call(departmentTypeListProcessor, request, ListDepartmentTypeResponse.class);
    }

    @Override
    public UpdateDepartmentTypeResponse updateDepartmentType(UpdateDepartmentTypeRequest request) {
        return call(departmentTypeUpdateProcessor, request, UpdateDepartmentTypeResponse.class);
    }

    @Override
    public DeleteDepartmentTypeResponse deleteDepartmentType(DeleteDepartmentTypeRequest request) {
        return call(departmentTypeDeleteProcessor, request, DeleteDepartmentTypeResponse.class);
    }
}
