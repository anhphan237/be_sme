package com.sme.be_sme.modules.platform.api.response;

import java.util.Date;

import lombok.Data;

@Data
public class PlatformCompanyDetailResponse {
    private String companyId;
    private String name;
    private String taxCode;
    private String address;
    private String status;
    private Date createdAt;
    private int userCount;
    private String subscriptionId;
    private String subscriptionStatus;
    private String planCode;
    private String planName;
    private Date currentPeriodEnd;
}
