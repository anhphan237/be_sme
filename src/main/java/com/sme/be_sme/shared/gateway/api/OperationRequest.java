package com.sme.be_sme.shared.gateway.api;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OperationRequest extends BaseRequest {
    private JsonNode payload;
}
