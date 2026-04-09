package com.sme.be_sme.modules.platform.api.request;

import lombok.Data;

@Data
public class PlatformForecastRequest {
    private String metric;
    private String startDate;
    private String endDate;
    private String groupBy;
    private Integer forecastPoints;
}
