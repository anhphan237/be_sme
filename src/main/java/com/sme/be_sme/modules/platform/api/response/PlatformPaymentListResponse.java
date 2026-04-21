package com.sme.be_sme.modules.platform.api.response;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class PlatformPaymentListResponse {
    private List<PaymentItem> items;
    private Integer total;
    private Integer page;
    private Integer size;

    @Data
    public static class PaymentItem {
        private String transactionId;
        private String companyId;
        private String companyName;

        private String subscriptionId;
        private String invoiceId;

        private String provider;
        private String providerTxnId;

        private Integer amount;
        private String currency;
        private String status;
        private String failureReason;

        private Date createdAt;
        private Date paidAt;
    }
}