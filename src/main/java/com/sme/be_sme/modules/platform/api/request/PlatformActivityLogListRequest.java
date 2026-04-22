package com.sme.be_sme.modules.platform.api.request;

import lombok.Data;

@Data
public class PlatformActivityLogListRequest {
    private Integer page;
    private Integer size;
    private String userId;
}
