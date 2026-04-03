package com.sme.be_sme.modules.platform.api.response;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class PlatformCompanyListResponse {
    private List<CompanyItem> items;
    private int total;

    @Data
    public static class CompanyItem {
        private String companyId;
        private String name;
        private String status;
        private Date createdAt;
        private int userCount;
        private String subscriptionStatus;
        private String planCode;
    }
}
