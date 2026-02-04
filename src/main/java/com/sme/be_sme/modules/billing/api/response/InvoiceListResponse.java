package com.sme.be_sme.modules.billing.api.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceListResponse {
    private List<InvoiceSummaryResponse> invoices;
}
