package com.sme.be_sme.modules.company.handler;

import com.sme.be_sme.modules.company.infrastructure.persistence.entity.DepartmentEntity;
import com.sme.be_sme.modules.company.infrastructure.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DepartmentHandler {

    private final DepartmentRepository departmentRepository;

    public void create(DepartmentEntity entity) {
        departmentRepository.insert(entity);
    }
}
