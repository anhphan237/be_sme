package com.sme.be_sme.modules.identity.context;

import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IdentityUpdateUserContext extends BizContext {
    private String userId;
    private String userEmail;
    private String fullName;
    private String roleCode;
    private String userStatus;

    public IdentityUpdateUserContext() {
        super();
    }
}
