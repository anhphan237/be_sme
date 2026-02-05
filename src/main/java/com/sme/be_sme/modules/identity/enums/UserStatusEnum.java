package com.sme.be_sme.modules.identity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    ACTIVE("ACTIVE", "User is active"),
    INACTIVE("INACTIVE", "User is inactive"),;

    private final String code;
    private final String description;
}
