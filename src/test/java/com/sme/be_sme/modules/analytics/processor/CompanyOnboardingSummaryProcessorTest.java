package com.sme.be_sme.modules.analytics.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.analytics.api.response.CompanyOnboardingSummaryResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyOnboardingSummaryProcessorTest {

    @Mock
    private OnboardingInstanceMapper onboardingInstanceMapper;

    @Test
    void summary_countsDoneInstance_whenCompletedAtWithinRange() {
        CompanyOnboardingSummaryProcessor processor = new CompanyOnboardingSummaryProcessor(
                new ObjectMapper(),
                onboardingInstanceMapper
        );

        BizContext context = new BizContext();
        context.setTenantId("c1");
        context.setPayload(new ObjectMapper()
                .createObjectNode()
                .put("startDate", "2026-04-01")
                .put("endDate", "2026-04-30"));

        OnboardingInstanceEntity done = new OnboardingInstanceEntity();
        done.setOnboardingId("ob1");
        done.setCompanyId("c1");
        done.setEmployeeId("e1");
        done.setStatus("DONE");
        done.setStartDate(Date.from(Instant.parse("2026-04-10T00:00:00Z")));
        done.setCompletedAt(Date.from(Instant.parse("2026-04-20T00:00:00Z")));

        OnboardingInstanceEntity active = new OnboardingInstanceEntity();
        active.setOnboardingId("ob2");
        active.setCompanyId("c1");
        active.setEmployeeId("e2");
        active.setStatus("ACTIVE");
        active.setStartDate(Date.from(Instant.parse("2026-04-12T00:00:00Z")));

        when(onboardingInstanceMapper.selectAll()).thenReturn(List.of(done, active));

        CompanyOnboardingSummaryResponse response =
                (CompanyOnboardingSummaryResponse) processor.execute(context);

        assertEquals(2, response.getTotalEmployees());
        assertEquals(1, response.getCompletedCount());
    }
}

