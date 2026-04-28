package com.sme.be_sme.modules.platform.api.response;

import lombok.Data;

@Data
public class DeletePlatformTemplateResponse {
    private String templateId;
    private Boolean deleted;
}