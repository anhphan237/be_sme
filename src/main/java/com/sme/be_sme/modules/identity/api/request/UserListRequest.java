package com.sme.be_sme.modules.identity.api.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Request for com.sme.identity.user.list.
 * tenantId from context; optional limit/offset for pagination.
 */
@Getter
@Setter
public class UserListRequest {
    private Integer limit;
    private Integer offset;
}
