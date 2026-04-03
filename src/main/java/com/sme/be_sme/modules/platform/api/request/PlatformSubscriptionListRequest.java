package com.sme.be_sme.modules.platform.api.request;

import lombok.Data;

@Data
public class PlatformSubscriptionListRequest {
    private Integer page;
    private Integer size;
    private String status;
    private String planCode;
}
