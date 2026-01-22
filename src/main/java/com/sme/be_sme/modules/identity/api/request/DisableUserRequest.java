package com.sme.be_sme.modules.identity.api.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisableUserRequest {
    private String userId;

    /**
     * If true -> INACTIVE
     * If false -> ACTIVE
     */
    private Boolean disabled;
}