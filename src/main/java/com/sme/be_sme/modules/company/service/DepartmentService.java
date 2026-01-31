package com.sme.be_sme.modules.company.service;

import com.sme.be_sme.modules.company.handler.DepartmentHandler;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.DepartmentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentHandler departmentHandler;

    public void createDepartment(DepartmentEntity entity) {
        departmentHandler.create(entity);
    }
}
