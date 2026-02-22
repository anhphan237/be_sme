package com.sme.be_sme.modules.identity.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.infrastructure.mapper.DepartmentMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.DepartmentEntity;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapperExt;
import com.sme.be_sme.modules.employee.infrastructure.persistence.entity.EmployeeProfileEntity;
import com.sme.be_sme.modules.identity.api.request.UserListRequest;
import com.sme.be_sme.modules.identity.api.response.UserListItem;
import com.sme.be_sme.modules.identity.api.response.UserListResponse;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.identity.infrastructure.repository.UserRoleRepository;
import com.sme.be_sme.modules.identity.service.UserService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class IdentityUserListProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final UserRoleRepository userRoleRepository;
    private final EmployeeProfileMapperExt employeeProfileMapperExt;
    private final DepartmentMapper departmentMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        UserListRequest request = payload != null && !payload.isEmpty()
                ? objectMapper.convertValue(payload, UserListRequest.class)
                : new UserListRequest();
        return process(context, request);
    }

    private UserListResponse process(BizContext context, UserListRequest request) {
        String companyId = context.getTenantId();
        if (companyId == null || companyId.isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        List<UserEntity> users = userService.listByCompanyId(companyId);
        List<UserListItem> items = new ArrayList<>(users.size());

        for (UserEntity u : users) {
            Set<String> roleSet = userRoleRepository.findRoles(companyId, u.getUserId());
            List<String> roles = roleSet == null ? List.of() : new ArrayList<>(roleSet);

            UserListItem item = new UserListItem();
            item.setUserId(u.getUserId());
            item.setEmail(u.getEmail());
            item.setFullName(u.getFullName());
            item.setPhone(u.getPhone());
            item.setStatus(u.getStatus());
            item.setRoles(roles);

            EmployeeProfileEntity profile = employeeProfileMapperExt.selectByCompanyIdAndUserId(companyId, u.getUserId());
            if (profile != null && profile.getDepartmentId() != null && !profile.getDepartmentId().isBlank()) {
                item.setDepartmentId(profile.getDepartmentId());
                DepartmentEntity dept = departmentMapper.selectByPrimaryKey(profile.getDepartmentId());
                if (dept != null) {
                    item.setDepartmentName(dept.getName());
                }
            }
            items.add(item);
        }

        if (request.getLimit() != null && request.getLimit() > 0) {
            int offset = request.getOffset() != null && request.getOffset() >= 0 ? request.getOffset() : 0;
            int limit = request.getLimit();
            int toIndex = Math.min(offset + limit, items.size());
            if (offset >= items.size()) {
                items = List.of();
            } else {
                items = new ArrayList<>(items.subList(offset, toIndex));
            }
        }

        UserListResponse response = new UserListResponse();
        response.setUsers(items);
        return response;
    }
}
