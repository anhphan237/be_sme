package com.sme.be_sme.modules.identity.infrastructure.repository.impl;

import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapper;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapperExt;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.identity.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;       // gen (insert/update)
    private final UserMapperExt userMapperExt; // manual (select theo company)

    @Override
    public Optional<UserEntity> findById(String companyId, String userId) {
        return Optional.ofNullable(userMapperExt.selectByCompanyIdAndUserId(companyId, userId));
    }

    @Override
    public Optional<UserEntity> findByCompanyIdAndEmail(String companyId, String email) {
        return Optional.ofNullable(userMapperExt.selectByCompanyIdAndEmail(companyId, email));
    }

    @Override
    public Optional<UserEntity> findByEmail(String email) {
        return Optional.ofNullable(userMapperExt.selectByEmail(email));
    }

    @Override
    public void insert(UserEntity entity) {
        userMapper.insert(entity);
    }

    @Override
    public void update(UserEntity entity) {
        userMapper.updateByPrimaryKey(entity);
    }
}
