package com.sme.be_sme.modules.billing.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DunningRetryResponse {
    private boolean success;
    private String message;
    /** When success: payment intent id for client to complete payment */
    private String paymentIntentId;
    /** When success: client secret for frontend */
    private String clientSecret;
    /** When success: gateway name */
    private String gateway;
    /** Updated retry count after this attempt */
    private Integer retryCount;
}
