package com.sme.be_sme.modules.billing.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class SubscriptionHistoryResponse {
    private List<Item> items;
    /** total rows matching filter (all pages) */
    private Integer total;
    /** current page index, 0-based */
    private Integer page;
    /** page size */
    private Integer size;
    /** total pages; 0 if total is 0 */
    private Integer totalPages;

    @Getter
    @Setter
    public static class Item {
        private String historyId;
        private String subscriptionId;
        private String oldPlanCode;
        private String newPlanCode;
        private String billingCycle;
        private String changedBy;
        private Date changedAt;
        private Date effectiveFrom;
        private Date effectiveTo;
    }
}
