package com.sme.be_sme.modules.platform.api.request;

import lombok.Data;

@Data
public class PlatformErrorLogListRequest {
    private Integer page;
    private Integer size;

    private String keyword;
    private String errorCode;
    private String severity;
    private String status;
    private String operationType;
    private String companyId;
    private String actorRole;

    private String startDate;
    private String endDate;
}
