package com.sme.be_sme.modules.company.facade;

import com.sme.be_sme.modules.company.api.request.CreateDepartmentRequest;
import com.sme.be_sme.modules.company.api.response.CreateDepartmentResponse;
import com.sme.be_sme.modules.company.processor.CreateDepartmentProcessor;
import com.sme.be_sme.shared.gateway.core.BizContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DepartmentFacadeImpl implements DepartmentFacade {

    private final CreateDepartmentProcessor processor;

    @Override
    public CreateDepartmentResponse createDepartment(CreateDepartmentRequest request) {
        return processor.process(BizContextHolder.get(), request);
    }
}
