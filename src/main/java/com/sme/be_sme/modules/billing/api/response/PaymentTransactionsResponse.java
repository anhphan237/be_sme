package com.sme.be_sme.modules.billing.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class PaymentTransactionsResponse {
    private List<TransactionItem> transactions;
    private Integer page;
    private Integer pageSize;
    private Long total;

    @Getter
    @Setter
    public static class TransactionItem {
        private String id;
        private String invoiceId;
        private Integer amount;
        private Double amountInDecimal;
        private String currency;
        private String status;
        private String type;
        private String paymentMethod;
        private String provider;
        private String providerTransactionId;
        private String description;
        private Date createdAt;
        private String companyId;
    }
}
