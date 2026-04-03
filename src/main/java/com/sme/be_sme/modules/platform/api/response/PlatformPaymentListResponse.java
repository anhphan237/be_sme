package com.sme.be_sme.modules.platform.api.response;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class PlatformPaymentListResponse {
    private List<PaymentItem> items;
    private int total;

    @Data
    public static class PaymentItem {
        private String transactionId;
        private String companyId;
        private String companyName;
        private Integer amount;
        private String currency;
        private String status;
        private Date createdAt;
    }
}
