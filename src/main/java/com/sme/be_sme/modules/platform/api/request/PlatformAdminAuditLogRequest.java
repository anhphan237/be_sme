package com.sme.be_sme.modules.platform.api.request;

import lombok.Data;

@Data
public class PlatformAdminAuditLogRequest {
    private Integer page;
    private Integer size;
    private String adminUserId;
    private String action;
    private String startDate;
    private String endDate;
}
