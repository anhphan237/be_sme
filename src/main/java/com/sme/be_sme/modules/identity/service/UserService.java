package com.sme.be_sme.modules.identity.service;

import com.sme.be_sme.modules.identity.handler.UserHandler;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserHandler userHandler;

    public Optional<UserEntity> findByCompanyIdAndEmail(String companyId, String email) {
        return userHandler.findByEmail(companyId, email);
    }

    public Optional<UserEntity> findByEmail(String email) {
        return userHandler.findByEmail(email);
    }

    public void createUser(UserEntity entity) {
        userHandler.create(entity);
    }

    public Optional<UserEntity> findById(String companyId, String userId) {
        return userHandler.findById(companyId, userId);
    }

    public void updateUser(UserEntity entity) {
        userHandler.update(entity);
    }
}
