package com.sme.be_sme.modules.platform.api.response;

import lombok.Data;

@Data
public class CreatePlatformTemplateResponse {
    private String templateId;
    private String name;
    private String status;
    private String templateKind;
    private String departmentTypeCode;
    private String level;
}
