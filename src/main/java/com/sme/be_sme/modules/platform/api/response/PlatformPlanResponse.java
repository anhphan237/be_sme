package com.sme.be_sme.modules.platform.api.response;

import lombok.Data;

@Data
public class PlatformPlanResponse {
    private String planId;
    private String code;
    private String name;
    private String status;
}
