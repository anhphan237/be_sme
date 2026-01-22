package com.sme.be_sme.modules.identity.api.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DisableUserResponse {
    private String userId;
    private String status; // ACTIVE / INACTIVE
}
