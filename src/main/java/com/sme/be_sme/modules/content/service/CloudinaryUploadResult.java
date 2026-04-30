package com.sme.be_sme.modules.content.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CloudinaryUploadResult {
    private final String secureUrl;
    private final Long bytes;
}
