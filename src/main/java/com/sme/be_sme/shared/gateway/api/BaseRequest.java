package com.sme.be_sme.shared.gateway.api;

import lombok.Data;

@Data
public abstract class BaseRequest {
    private String operationType; // com.sme.xxx...
    private String requestId;
    private String tenantId; // company_id dạng String
}
