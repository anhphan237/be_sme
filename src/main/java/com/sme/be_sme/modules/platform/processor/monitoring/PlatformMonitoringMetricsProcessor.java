package com.sme.be_sme.modules.platform.processor.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.platform.api.request.PlatformMonitoringMetricsRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformMonitoringMetricsResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformMonitoringMetricsProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        objectMapper.convertValue(payload, PlatformMonitoringMetricsRequest.class);

        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();

        Double cpuUsagePercent = getCpuUsagePercent();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        PlatformMonitoringMetricsResponse response = new PlatformMonitoringMetricsResponse();
        response.setUsedMemoryMb(used / 1024 / 1024);
        response.setMaxMemoryMb(max / 1024 / 1024);
        response.setHeapUsagePercent(max > 0 ? (used * 100.0 / max) : null);
        response.setThreadCount(threadMXBean.getThreadCount());
        response.setAvailableProcessors(runtime.availableProcessors());
        response.setCpuUsagePercent(cpuUsagePercent);
        response.setCpuUsagePercent(getCpuUsagePercent());
        return response;
    }

    private Double getCpuUsagePercent() {
        try {
            java.lang.management.OperatingSystemMXBean osBean =
                    ManagementFactory.getOperatingSystemMXBean();

            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                double load = ((com.sun.management.OperatingSystemMXBean) osBean).getSystemCpuLoad();
                if (load >= 0) {
                    return load * 100.0;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }


}