package com.sme.be_sme.modules.identity.infrastructure.repository;

import java.util.Set;

public interface UserRoleRepository {
    Set<String> findRoles(String companyId, String userId);
}
