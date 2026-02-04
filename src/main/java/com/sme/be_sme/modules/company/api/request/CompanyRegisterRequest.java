package com.sme.be_sme.modules.company.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyRegisterRequest {
    private CompanyInfo company;
    private AdminInfo admin;

    @Getter
    @Setter
    public static class CompanyInfo {
        private String name;
        private String taxCode;
        private String address;
        private String timezone;
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
