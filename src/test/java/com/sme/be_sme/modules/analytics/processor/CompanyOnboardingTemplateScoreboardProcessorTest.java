package com.sme.be_sme.modules.analytics.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.analytics.api.response.CompanyOnboardingTemplateScoreboardResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapperExt;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyOnboardingTemplateScoreboardProcessorTest {

    @Mock
    private OnboardingInstanceMapperExt onboardingInstanceMapperExt;
    @Mock
    private TaskInstanceMapper taskInstanceMapper;

    @Test
    void scoreboard_filtersDoneStatus_andSetsFitLevel() {
        CompanyOnboardingTemplateScoreboardProcessor processor = new CompanyOnboardingTemplateScoreboardProcessor(
                new ObjectMapper(),
                onboardingInstanceMapperExt,
                taskInstanceMapper
        );

        BizContext context = new BizContext();
        context.setTenantId("c1");
        context.setPayload(new ObjectMapper()
                .createObjectNode()
                .put("templateId", "TPL-1")
                .put("status", "DONE")
                .put("limit", 10));

        OnboardingInstanceEntity done = new OnboardingInstanceEntity();
        done.setOnboardingId("ob-done");
        done.setCompanyId("c1");
        done.setOnboardingTemplateId("TPL-1");
        done.setStatus("DONE");
        done.setEmployeeId("e1");
        done.setProgressPercent(100);

        OnboardingInstanceEntity active = new OnboardingInstanceEntity();
        active.setOnboardingId("ob-active");
        active.setCompanyId("c1");
        active.setOnboardingTemplateId("TPL-1");
        active.setStatus("ACTIVE");
        active.setEmployeeId("e2");
        active.setProgressPercent(80);

        when(onboardingInstanceMapperExt.selectByCompanyTemplateAndStatus("c1", "TPL-1", "DONE"))
                .thenReturn(List.of(done));

        TaskInstanceEntity doneTask = new TaskInstanceEntity();
        doneTask.setStatus("DONE");
        doneTask.setDueDate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L));
        doneTask.setCompletedAt(new Date());
        when(taskInstanceMapper.selectByCompanyIdAndOnboardingId("c1", "ob-done"))
                .thenReturn(List.of(doneTask));

        CompanyOnboardingTemplateScoreboardResponse response =
                (CompanyOnboardingTemplateScoreboardResponse) processor.execute(context);

        assertEquals(1, response.getTotalCandidates());
        assertEquals(1, response.getCandidates().size());
        assertEquals("ob-done", response.getCandidates().get(0).getInstanceId());
        assertEquals("HIGH", response.getCandidates().get(0).getFitLevel());
    }
}

