package com.sme.be_sme.modules.identity.infrastructure.repository;

import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    List<UserEntity> findByCompanyId(String companyId);
    Optional<UserEntity> findById(String companyId, String userId);
    Optional<UserEntity> findByCompanyIdAndEmail(String companyId, String email);
    Optional<UserEntity> findByEmail(String email);
    void insert(UserEntity entity);
    void update(UserEntity entity);
}
