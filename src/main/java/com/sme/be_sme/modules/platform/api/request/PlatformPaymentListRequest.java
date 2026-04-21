package com.sme.be_sme.modules.platform.api.request;

import lombok.Data;

@Data
public class PlatformPaymentListRequest {
    private Integer page;
    private Integer size;

    private String companyId;
    private String subscriptionId;
    private String invoiceId;
    private String status;

    private String startDate;
    private String endDate;
}