package com.sme.be_sme.modules.platform.processor.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapper;
import com.sme.be_sme.modules.platform.api.request.PlatformSystemHealthRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformSystemHealthResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformSystemHealthProcessor extends BaseBizProcessor<BizContext> {

    private static final String PLATFORM_STATUS = "PLATFORM";

    private final ObjectMapper objectMapper;
    private final CompanyMapper companyMapper;
    private final UserMapper userMapper;
    private final DataSource dataSource;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        objectMapper.convertValue(payload, PlatformSystemHealthRequest.class);

        String dbStatus;
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
            dbStatus = "OK";
        } catch (Exception e) {
            dbStatus = "ERROR";
        }

        List<CompanyEntity> allCompanies = companyMapper.selectAll();
        int totalCompanies = 0;
        for (CompanyEntity company : allCompanies) {
            if (company != null && !PLATFORM_STATUS.equalsIgnoreCase(company.getStatus())) {
                totalCompanies++;
            }
        }

        int totalUsers = userMapper.selectAll().size();

        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        String uptime = formatUptime(uptimeMs);

        PlatformSystemHealthResponse response = new PlatformSystemHealthResponse();
        response.setDbStatus(dbStatus);
        response.setActiveConnections(0);
        response.setTotalCompanies(totalCompanies);
        response.setTotalUsers(totalUsers);
        response.setUptime(uptime);
        return response;
    }

    private String formatUptime(long millis) {
        long seconds = millis / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%dd %dh %dm %ds", days, hours, minutes, secs);
    }
}
