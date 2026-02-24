package com.sme.be_sme.modules.billing.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class PaymentTransactionsResponse {
    private List<TransactionItem> transactions;

    @Getter
    @Setter
    public static class TransactionItem {
        private String id;
        private String invoiceId;
        private Integer amount;
        private String currency;
        private String status;
        private String provider;
        private Date createdAt;
        private String companyId;
    }
}
