package com.sme.be_sme.modules.company.facade;

import com.sme.be_sme.modules.company.api.request.CreateDepartmentRequest;
import com.sme.be_sme.modules.company.api.request.ListDepartmentRequest;
import com.sme.be_sme.modules.company.api.request.UpdateDepartmentRequest;
import com.sme.be_sme.modules.company.api.response.CreateDepartmentResponse;
import com.sme.be_sme.modules.company.api.response.ListDepartmentResponse;
import com.sme.be_sme.modules.company.api.response.UpdateDepartmentResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface DepartmentFacade extends OperationFacadeProvider {

    @OperationType("com.sme.company.department.create")
    CreateDepartmentResponse createDepartment(CreateDepartmentRequest request);

    @OperationType("com.sme.company.department.update")
    UpdateDepartmentResponse updateDepartment(UpdateDepartmentRequest request);

    @OperationType("com.sme.company.department.list")
    ListDepartmentResponse listDepartments(ListDepartmentRequest request);

    @OperationType("com.sme.org.department.create")
    CreateDepartmentResponse createOrgDepartment(CreateDepartmentRequest request);

    @OperationType("com.sme.org.department.update")
    UpdateDepartmentResponse updateOrgDepartment(UpdateDepartmentRequest request);
}
