package com.sme.be_sme.modules.billing.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentTransactionsRequest {
    /**
     * 1-based page index. Defaults to 1.
     */
    private Integer page;

    /**
     * Number of items per page. Defaults to 20.
     */
    private Integer pageSize;
}
