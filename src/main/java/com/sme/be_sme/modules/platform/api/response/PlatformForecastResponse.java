package com.sme.be_sme.modules.platform.api.response;

import java.util.List;
import lombok.Data;

@Data
public class PlatformForecastResponse {
    private String metric;
    private String groupBy;
    private String method;
    private String confidenceNote;
    private List<PointItem> historical;
    private List<PointItem> forecast;

    @Data
    public static class PointItem {
        private String bucket;
        private Double value;
    }
}
