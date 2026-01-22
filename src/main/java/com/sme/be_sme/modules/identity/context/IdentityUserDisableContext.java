package com.sme.be_sme.modules.identity.context;

import com.sme.be_sme.modules.identity.api.request.DisableUserRequest;
import com.sme.be_sme.modules.identity.api.response.DisableUserResponse;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IdentityUserDisableContext {
    private BizContext biz;
    private DisableUserRequest request;
    private DisableUserResponse response;

    // shared state
    private String status;
}
