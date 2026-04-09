package com.sme.be_sme.modules.platform.api.response;

import java.util.List;
import lombok.Data;

@Data
public class PlatformCompanyTrendResponse {
    private String startDate;
    private String endDate;
    private String groupBy;
    private List<TrendItem> items;

    @Data
    public static class TrendItem {
        private String bucket;
        private Integer value;
        private Integer previousValue;
        private Double growthRate;
    }
}
