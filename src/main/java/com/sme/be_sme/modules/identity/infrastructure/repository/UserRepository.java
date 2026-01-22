package com.sme.be_sme.modules.identity.infrastructure.repository;

import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;

import java.util.Optional;

public interface UserRepository {
    Optional<UserEntity> findById(String companyId, String userId);
    Optional<UserEntity> findByEmail(String companyId, String email);
    void insert(UserEntity entity);
    void update(UserEntity entity);
}
