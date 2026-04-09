package com.sme.be_sme.modules.platform.api.response;

import java.util.List;
import lombok.Data;

@Data
public class PlatformOnboardingTrendResponse {
    private String startDate;
    private String endDate;
    private String groupBy;
    private List<TrendItem> items;

    @Data
    public static class TrendItem {
        private String bucket;
        private Integer total;
        private Integer active;
        private Integer completed;
        private Integer risk;
        private Integer previousTotal;
        private Double growthRate;
    }
}
