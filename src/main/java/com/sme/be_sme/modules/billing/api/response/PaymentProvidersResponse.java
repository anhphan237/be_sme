package com.sme.be_sme.modules.billing.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class PaymentProvidersResponse {
    private List<ProviderItem> providers;

    @Getter
    @Setter
    public static class ProviderItem {
        private String name;
        private String status;
        private String accountId;
        private Date lastSync;
    }
}
