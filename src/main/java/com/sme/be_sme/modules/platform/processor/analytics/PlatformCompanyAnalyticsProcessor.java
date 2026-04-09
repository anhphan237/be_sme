package com.sme.be_sme.modules.platform.processor.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformCompanyAnalyticsRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformCompanyAnalyticsResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformCompanyAnalyticsProcessor extends BaseBizProcessor<BizContext> {

    private static final String STATUS_PLATFORM = "PLATFORM";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_SUSPENDED = "SUSPENDED";

    private final ObjectMapper objectMapper;
    private final CompanyMapper companyMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformCompanyAnalyticsRequest request = objectMapper.convertValue(payload, PlatformCompanyAnalyticsRequest.class);

        Date startDate = PlatformAnalyticsSupport.parseDate(request.getStartDate(), true);
        Date endDate = PlatformAnalyticsSupport.parseDate(request.getEndDate(), false);

        List<CompanyEntity> allCompanies = companyMapper.selectAll();

        int totalCompanies = 0;
        int activeCompanies = 0;
        int inactiveCompanies = 0;
        int suspendedCompanies = 0;
        int newCompanies = 0;
        int companiesAtStart = 0;

        for (CompanyEntity company : allCompanies) {
            if (company == null || STATUS_PLATFORM.equalsIgnoreCase(company.getStatus())) {
                continue;
            }

            totalCompanies++;

            if (STATUS_ACTIVE.equalsIgnoreCase(company.getStatus())) {
                activeCompanies++;
            } else if (STATUS_SUSPENDED.equalsIgnoreCase(company.getStatus())) {
                suspendedCompanies++;
            } else {
                inactiveCompanies++;
            }

            if (company.getCreatedAt() != null && startDate != null && company.getCreatedAt().before(startDate)) {
                companiesAtStart++;
            }
            if (PlatformAnalyticsSupport.inRange(company.getCreatedAt(), startDate, endDate)) {
                newCompanies++;
            }
        }

        PlatformCompanyAnalyticsResponse response = new PlatformCompanyAnalyticsResponse();
        response.setTotalCompanies(totalCompanies);
        response.setActiveCompanies(activeCompanies);
        response.setInactiveCompanies(inactiveCompanies);
        response.setSuspendedCompanies(suspendedCompanies);
        response.setNewCompanies(newCompanies);
        response.setGrowthRate(companiesAtStart > 0 ? (double) newCompanies / companiesAtStart : 0.0);
        return response;
    }
}
