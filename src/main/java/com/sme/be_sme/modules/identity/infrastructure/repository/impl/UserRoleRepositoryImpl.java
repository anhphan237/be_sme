package com.sme.be_sme.modules.identity.infrastructure.repository.impl;

import com.sme.be_sme.modules.identity.infrastructure.repository.UserRoleRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public class UserRoleRepositoryImpl implements UserRoleRepository {
    @Override
    public Set<String> findRoles(String companyId, String userId) {
        return Set.of("ADMIN"); // TODO: replace by DB user_roles
    }
}
