package com.sme.be_sme.modules.identity.handler;

import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.identity.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserHandler {

    private final UserRepository userRepository;

    public List<UserEntity> listByCompanyId(String companyId) {
        return userRepository.findByCompanyId(companyId);
    }

    public Optional<UserEntity> findByEmail(String companyId, String email) {
        return userRepository.findByCompanyIdAndEmail(companyId, email);
    }

    public Optional<UserEntity> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<UserEntity> findById(String companyId, String userId) {
        return userRepository.findById(companyId, userId);
    }

    public void create(UserEntity entity) {
        userRepository.insert(entity);
    }

    public void update(UserEntity entity) {
        userRepository.update(entity);
    }
}
