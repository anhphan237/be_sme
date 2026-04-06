package com.sme.be_sme.modules.platform.processor.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformCompanyAnalyticsRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformCompanyAnalyticsResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
        PlatformCompanyAnalyticsRequest request =
                objectMapper.convertValue(payload, PlatformCompanyAnalyticsRequest.class);

        Date startDate = parseDate(request.getStartDate(), true);
        Date endDate = parseDate(request.getEndDate(), false);

        List<CompanyEntity> allCompanies = companyMapper.selectAll();

        int totalCompanies = 0;
        int activeCompanies = 0;
        int inactiveCompanies = 0;
        int suspendedCompanies = 0;
        int newCompanies = 0;
        int companiesAtStart = 0;

        for (CompanyEntity company : allCompanies) {
            if (company == null) {
                continue;
            }

            String status = company.getStatus();
            if (STATUS_PLATFORM.equalsIgnoreCase(status)) {
                continue;
            }

            totalCompanies++;

            if (STATUS_ACTIVE.equalsIgnoreCase(status)) {
                activeCompanies++;
            } else if (STATUS_SUSPENDED.equalsIgnoreCase(status)) {
                suspendedCompanies++;
            } else {
                inactiveCompanies++;
            }

            if (company.getCreatedAt() != null) {
                if (startDate != null && company.getCreatedAt().before(startDate)) {
                    companiesAtStart++;
                }

                if (inRange(company.getCreatedAt(), startDate, endDate)) {
                    newCompanies++;
                }
            }
        }

        PlatformCompanyAnalyticsResponse response = new PlatformCompanyAnalyticsResponse();
        response.setTotalCompanies(totalCompanies);
        response.setActiveCompanies(activeCompanies);
        response.setInactiveCompanies(inactiveCompanies);
        response.setSuspendedCompanies(suspendedCompanies);
        response.setNewCompanies(newCompanies);
        response.setGrowthRate(companiesAtStart > 0 ? (double) newCompanies / companiesAtStart : null);
        return response;
    }

    private boolean inRange(Date value, Date start, Date end) {
        if (value == null) {
            return false;
        }
        if (start != null && value.before(start)) {
            return false;
        }
        if (end != null && !value.before(end)) {
            return false;
        }
        return true;
    }

    private Date parseDate(String isoDate, boolean startOfDay) {
        if (!StringUtils.hasText(isoDate)) {
            return null;
        }
        LocalDate ld = LocalDate.parse(isoDate);
        if (startOfDay) {
            return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        return Date.from(ld.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}