package com.sme.be_sme.modules.company.api.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Company self-registration. Initial subscription is always the global FREE plan; upgrade via billing after payment.
 */
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
        /** Company code (3 chars) for employee format [ABC]000001. Optional; derived from name if null. */
        private String code;
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
