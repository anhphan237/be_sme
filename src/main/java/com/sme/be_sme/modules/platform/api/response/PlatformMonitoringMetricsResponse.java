package com.sme.be_sme.modules.platform.api.response;

import lombok.Data;

@Data
public class PlatformMonitoringMetricsResponse {
    private Double cpuUsagePercent;
    private Double heapUsagePercent;
    private Long usedMemoryMb;
    private Long maxMemoryMb;
    private Integer threadCount;
    private Integer availableProcessors;
}
