package com.sme.be_sme.modules.identity.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckEmailResponse {
    /** true if email already exists (trùng), false if available */
    private boolean exists;
}
