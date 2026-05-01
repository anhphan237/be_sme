package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapper;
import com.sme.be_sme.modules.identity.processor.IdentityUserUpdateProcessor;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.service.ManagerOnboardingEvaluationService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BizContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingInstanceCompleteProcessorTest {

    @Mock
    private OnboardingInstanceMapper onboardingInstanceMapper;
    @Mock
    private TaskInstanceMapper taskInstanceMapper;
    @Mock
    private EmployeeProfileMapper employeeProfileMapper;
    @Mock
    private IdentityUserUpdateProcessor identityUserUpdateProcessor;

    @Mock
    private ManagerOnboardingEvaluationService managerOnboardingEvaluationService;
    @Test
    void complete_rejectsWhenAnyTaskPending() {
        OnboardingInstanceCompleteProcessor processor = new OnboardingInstanceCompleteProcessor(
                new ObjectMapper(),
                onboardingInstanceMapper,
                taskInstanceMapper,
                employeeProfileMapper,
                identityUserUpdateProcessor,
                managerOnboardingEvaluationService
        );
        BizContext context = new BizContext();
        context.setTenantId("c1");
        context.setOperatorId("u1");
        context.setPayload(new ObjectMapper().createObjectNode().put("instanceId", "ob1"));

        OnboardingInstanceEntity instance = new OnboardingInstanceEntity();
        instance.setOnboardingId("ob1");
        instance.setCompanyId("c1");
        when(onboardingInstanceMapper.selectByPrimaryKey("ob1")).thenReturn(instance);

        TaskInstanceEntity pendingTask = new TaskInstanceEntity();
        pendingTask.setStatus("TODO");
        when(taskInstanceMapper.selectByCompanyIdAndOnboardingId("c1", "ob1"))
                .thenReturn(List.of(pendingTask));

        AppException ex = assertThrows(AppException.class, () -> processor.execute(context));
        assertEquals(ErrorCodes.BAD_REQUEST, ex.getCode());
        verify(onboardingInstanceMapper, never()).updateByPrimaryKey(instance);
        verify(managerOnboardingEvaluationService, never()).sendAfterOnboardingCompleted(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }
}