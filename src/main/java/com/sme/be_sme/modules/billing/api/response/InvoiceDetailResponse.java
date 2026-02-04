package com.sme.be_sme.modules.billing.api.response;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceDetailResponse {
    private String invoiceId;
    private String invoiceNo;
    private String subscriptionId;
    private Integer amountTotal;
    private String currency;
    private String status;
    private Date issuedAt;
    private Date dueAt;
    private String eInvoiceUrl;
}
