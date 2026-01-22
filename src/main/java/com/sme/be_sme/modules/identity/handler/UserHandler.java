package com.sme.be_sme.modules.identity.handler;

import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.identity.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserHandler {

    private final UserRepository userRepository;

    public Optional<UserEntity> findByEmail(String companyId, String email) {
        return userRepository.findByEmail(companyId, email);
    }

    public void create(UserEntity entity) {
        userRepository.insert(entity);
    }

    public void update(UserEntity entity) {
        userRepository.update(entity);
    }
}
