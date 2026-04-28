package com.sme.be_sme.modules.platform.api.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UpdatePlatformTemplateRequest extends CreatePlatformTemplateRequest {
    private String templateId;
}