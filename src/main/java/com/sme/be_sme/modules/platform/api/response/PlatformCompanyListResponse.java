package com.sme.be_sme.modules.platform.api.response;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class PlatformCompanyListResponse {

    private List<CompanyItem> items;
    private Integer total;

    @Data
    public static class CompanyItem {
        private String companyId;
        private String name;
        private String status;
        private Date createdAt;

        private Integer userCount;

        private String subscriptionId;
        private String subscriptionStatus;
        private String billingCycle;

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

        /**
         * Max percent among:
         * onboarded/month, onboarding templates, event templates, documents, storage.
         */
        private BigDecimal overallUsagePercent;

        /**
         * NO_PLAN, NONE, LOW, MEDIUM, HIGH, OVER_LIMIT
         */
        private String usageLevel;
    }
}