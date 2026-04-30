package com.sme.be_sme.modules.analytics.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.analytics.api.response.CandidateFitAssessResponse;
import com.sme.be_sme.modules.analytics.infrastructure.mapper.CandidateFitAuditMapper;
import com.sme.be_sme.modules.analytics.infrastructure.persistence.entity.CandidateFitAuditEntity;
import com.sme.be_sme.shared.gateway.core.BizContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateFitAssessProcessorTest {

    @Mock
    private CandidateFitAuditMapper candidateFitAuditMapper;

    @Test
    void assessCandidateFit_calculatesWeightedScore_andWritesAudit() {
        CandidateFitAssessProcessor processor = new CandidateFitAssessProcessor(
                new ObjectMapper(),
                candidateFitAuditMapper
        );
        BizContext context = new BizContext();
        context.setTenantId("c1");
        context.setOperatorId("hr1");
        context.setPayload(new ObjectMapper().createObjectNode()
                .put("employeeId", "e1")
                .put("jdMatchScore", 90)
                .put("competencyScore", 80)
                .put("interviewScore", 70));

        when(candidateFitAuditMapper.insert(any(CandidateFitAuditEntity.class))).thenReturn(1);

        CandidateFitAssessResponse response = (CandidateFitAssessResponse) processor.execute(context);

        assertEquals("e1", response.getEmployeeId());
        assertEquals(81.5d, response.getFitScore());
        assertEquals("HIGH", response.getFitLevel());
        assertEquals("HIRING_CANDIDATE", response.getFitType());

        ArgumentCaptor<CandidateFitAuditEntity> captor = ArgumentCaptor.forClass(CandidateFitAuditEntity.class);
        verify(candidateFitAuditMapper).insert(captor.capture());
        assertEquals("e1", captor.getValue().getEmployeeId());
        assertEquals(81.5d, captor.getValue().getFitScore());
    }
}

