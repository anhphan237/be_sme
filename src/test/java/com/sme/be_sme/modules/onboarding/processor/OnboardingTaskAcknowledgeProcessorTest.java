package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.service.OnboardingInstanceProgressService;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskWorkflow;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BizContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingTaskAcknowledgeProcessorTest {

    @Mock
    private TaskInstanceMapper taskInstanceMapper;
    @Mock
    private OnboardingInstanceProgressService progressService;

    @Test
    void acknowledge_rejects_doneTask() {
        OnboardingTaskAcknowledgeProcessor processor =
                new OnboardingTaskAcknowledgeProcessor(new ObjectMapper(), taskInstanceMapper, progressService);
        BizContext context = buildContext();
        context.setPayload(new ObjectMapper().createObjectNode().put("taskId", "t1"));

        TaskInstanceEntity task = baseTask();
        task.setStatus(OnboardingTaskWorkflow.STATUS_DONE);
        when(taskInstanceMapper.selectByPrimaryKey("t1")).thenReturn(task);

        AppException ex = assertThrows(AppException.class, () -> processor.execute(context));
        assertEquals(ErrorCodes.BAD_REQUEST, ex.getCode());
    }

    @Test
    void acknowledge_isIdempotent_whenAlreadyWaitAck() {
        OnboardingTaskAcknowledgeProcessor processor =
                new OnboardingTaskAcknowledgeProcessor(new ObjectMapper(), taskInstanceMapper, progressService);
        BizContext context = buildContext();
        context.setPayload(new ObjectMapper().createObjectNode().put("taskId", "t1"));

        TaskInstanceEntity task = baseTask();
        task.setStatus(OnboardingTaskWorkflow.STATUS_WAIT_ACK);
        task.setAcknowledgedAt(new java.util.Date());
        when(taskInstanceMapper.selectByPrimaryKey("t1")).thenReturn(task);

        OnboardingTaskResponse response = (OnboardingTaskResponse) processor.execute(context);
        assertEquals("t1", response.getTaskId());
        assertEquals(OnboardingTaskWorkflow.STATUS_WAIT_ACK, response.getStatus());
        verify(taskInstanceMapper, never()).updateByPrimaryKey(task);
    }

    private static BizContext buildContext() {
        BizContext context = new BizContext();
        context.setTenantId("c1");
        context.setOperatorId("u1");
        context.setRoles(Set.of("EMPLOYEE"));
        return context;
    }

    private static TaskInstanceEntity baseTask() {
        TaskInstanceEntity task = new TaskInstanceEntity();
        task.setTaskId("t1");
        task.setCompanyId("c1");
        task.setAssignedUserId("u1");
        task.setRequireAck(true);
        return task;
    }
}

