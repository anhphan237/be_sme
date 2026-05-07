package com.sme.be_sme.modules.platform.api.response;

import java.math.BigDecimal;
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

    private Integer userCount;

    private String subscriptionId;
    private String subscriptionStatus;
    private String billingCycle;
    private Date currentPeriodEnd;

    private String planId;
    private String planCode;
    private String planName;
    private String planStatus;

    private Integer employeeLimitPerMonth;
    private Integer onboardedThisMonth;
    private BigDecimal employeeUsagePercent;

    private Integer onboardingTemplateCount;
    private Integer onboardingTemplateLimit;
    private BigDecimal onboardingTemplateUsagePercent;

    private Integer eventTemplateCount;
    private Integer eventTemplateLimit;
    private BigDecimal eventTemplateUsagePercent;

    private Integer documentCount;
    private Integer documentLimit;
    private BigDecimal documentUsagePercent;

    private Long storageUsedBytes;
    private BigDecimal storageUsedMb;
    private BigDecimal storageUsedGb;

    private Long storageLimitBytes;
    private BigDecimal storageLimitMb;
    private BigDecimal storageLimitGb;
    private BigDecimal storageUsagePercent;

    private Integer activeOnboardingCount;

    private BigDecimal overallUsagePercent;
    private String usageLevel;
}