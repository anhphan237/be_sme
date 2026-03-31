package com.sme.be_sme.modules.company.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyRegisterRequest {
    private CompanyInfo company;
    private AdminInfo admin;
    /** Plan code for initial subscription (e.g. FREE, BASIC). Must be a valid global plan from plan list. */
    private String planCode;

    @Getter
    @Setter
    public static class CompanyInfo {
        private String name;
        private String taxCode;
        /** Company code (3 chars) for employee format [ABC]000001. Optional; derived from name if null. */
        private String code;
        private String address;
        private String timezone;
        /** Industry/sector of the company (e.g. Công nghệ, Bán lẻ, Sản xuất). Used by AI to generate onboarding template. */
        private String industry;
        /** Company size: STARTUP, SME, ENTERPRISE. Used by AI to generate onboarding template. */
        private String companySize;
    }

    @Getter
    @Setter
    public static class AdminInfo {
        private String username;
        private String password;
        private String fullName;
        private String phone;
    }
}
