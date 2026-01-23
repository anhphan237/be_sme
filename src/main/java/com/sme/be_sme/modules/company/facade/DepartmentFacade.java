package com.sme.be_sme.modules.company.facade;

import com.sme.be_sme.modules.company.api.request.CreateDepartmentRequest;
import com.sme.be_sme.modules.company.api.response.CreateDepartmentResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface DepartmentFacade extends OperationFacadeProvider {

    @OperationType("com.sme.company.department.create")
    CreateDepartmentResponse createDepartment(CreateDepartmentRequest request);
}
