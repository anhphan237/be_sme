package com.sme.be_sme.shared.gateway.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationStubResponse {
    private String operationType;
    private String code;
    private String message;

    public static OperationStubResponse notImplemented(String operationType) {
        return new OperationStubResponse(operationType, "NOT_IMPLEMENTED", "Operation is not implemented yet.");
    }
}
