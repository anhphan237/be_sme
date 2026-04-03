package com.sme.be_sme.modules.platform.api.response;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class PlatformInvoiceListResponse {
    private List<InvoiceItem> items;
    private int total;

    @Data
    public static class InvoiceItem {
        private String invoiceId;
        private String invoiceNo;
        private String companyId;
        private String companyName;
        private Integer amountTotal;
        private String currency;
        private String status;
        private Date dueAt;
    }
}
