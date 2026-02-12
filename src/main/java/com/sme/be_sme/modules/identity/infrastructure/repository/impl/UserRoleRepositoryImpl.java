package com.sme.be_sme.modules.identity.infrastructure.repository.impl;

import com.sme.be_sme.modules.identity.infrastructure.mapper.UserRoleMapperExt;
import com.sme.be_sme.modules.identity.infrastructure.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserRoleRepositoryImpl implements UserRoleRepository {

    private final UserRoleMapperExt userRoleMapperExt;

    @Override
    public Set<String> findRoles(String companyId, String userId) {
        List<String> codes = userRoleMapperExt.selectRoleCodesByCompanyAndUser(companyId, userId);
        if (codes == null) {
            return Set.of();
        }
        return codes.stream()
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toSet());
    }
}
