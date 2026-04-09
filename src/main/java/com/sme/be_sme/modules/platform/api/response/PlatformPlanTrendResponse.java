package com.sme.be_sme.modules.platform.api.response;

import java.util.List;
import lombok.Data;

@Data
public class PlatformPlanTrendResponse {
    private String startDate;
    private String endDate;
    private String groupBy;
    private List<PlanTrendItem> items;

    @Data
    public static class PlanTrendItem {
        private String bucket;
        private List<PlanValueItem> plans;
    }

    @Data
    public static class PlanValueItem {
        private String planId;
        private String planCode;
        private String planName;
        private Integer value;
        private Integer previousValue;
        private Double growthRate;
    }
}
