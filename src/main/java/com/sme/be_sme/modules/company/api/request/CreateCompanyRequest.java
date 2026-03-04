package com.sme.be_sme.modules.company.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCompanyRequest {
    private String companyId;
    private String name;
    private String taxCode;
    /** Company code (3 chars) for employee format [ABC]000001. Optional. */
    private String code;
    private String address;
    private String timezone;
    private String status;
}
