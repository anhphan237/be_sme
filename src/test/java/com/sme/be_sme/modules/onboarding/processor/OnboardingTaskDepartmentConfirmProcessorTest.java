package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.infrastructure.mapper.DepartmentMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.DepartmentEntity;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskDepartmentConfirmResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskDepartmentCheckpointMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskDepartmentCheckpointEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.service.OnboardingInstanceProgressService;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskActivityLogService;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskWorkflow;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BizContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingTaskDepartmentConfirmProcessorTest {

    @Mock
    private TaskInstanceMapper taskInstanceMapper;
    @Mock
    private TaskDepartmentCheckpointMapper taskDepartmentCheckpointMapper;
    @Mock
    private DepartmentMapper departmentMapper;
    @Mock
    private OnboardingTaskActivityLogService activityLogService;
    @Mock
    private OnboardingInstanceProgressService progressService;

    @Test
    void confirm_rejectedWhenOperatorNotDepartmentManager() {
        OnboardingTaskDepartmentConfirmProcessor processor = new OnboardingTaskDepartmentConfirmProcessor(
                new ObjectMapper(),
                taskInstanceMapper,
                taskDepartmentCheckpointMapper,
                departmentMapper,
                activityLogService,
                progressService
        );
        BizContext context = context("u-manager");
        context.setPayload(new ObjectMapper().createObjectNode()
                .put("taskId", "t1")
                .put("departmentId", "d1")
                .put("evidenceNote", "done"));

        TaskInstanceEntity task = task("t1");
        when(taskInstanceMapper.selectByPrimaryKey("t1")).thenReturn(task);

        TaskDepartmentCheckpointEntity checkpoint = checkpoint("t1", "d1");
        when(taskDepartmentCheckpointMapper.selectByCompanyIdAndTaskIdAndDepartmentId("c1", "t1", "d1"))
                .thenReturn(checkpoint);

        DepartmentEntity department = new DepartmentEntity();
        department.setDepartmentId("d1");
        department.setCompanyId("c1");
        department.setManagerUserId("u-other");
        when(departmentMapper.selectByPrimaryKey("d1")).thenReturn(department);

        assertThrows(AppException.class, () -> processor.execute(context));
        verify(taskDepartmentCheckpointMapper, never()).updateByPrimaryKey(any(TaskDepartmentCheckpointEntity.class));
    }

    @Test
    void confirm_lastCheckpoint_autoCompletesTask() {
        OnboardingTaskDepartmentConfirmProcessor processor = new OnboardingTaskDepartmentConfirmProcessor(
                new ObjectMapper(),
                taskInstanceMapper,
                taskDepartmentCheckpointMapper,
                departmentMapper,
                activityLogService,
                progressService
        );
        BizContext context = context("u-manager");
        context.setPayload(new ObjectMapper().createObjectNode()
                .put("taskId", "t1")
                .put("departmentId", "d1")
                .put("evidenceNote", "provided docs"));

        TaskInstanceEntity task = task("t1");
        task.setRequiresManagerApproval(true);
        when(taskInstanceMapper.selectByPrimaryKey("t1")).thenReturn(task);
        when(taskInstanceMapper.updateByPrimaryKey(any(TaskInstanceEntity.class))).thenReturn(1);

        TaskDepartmentCheckpointEntity checkpoint = checkpoint("t1", "d1");
        when(taskDepartmentCheckpointMapper.selectByCompanyIdAndTaskIdAndDepartmentId("c1", "t1", "d1"))
                .thenReturn(checkpoint);
        when(taskDepartmentCheckpointMapper.updateByPrimaryKey(any(TaskDepartmentCheckpointEntity.class))).thenReturn(1);
        when(taskDepartmentCheckpointMapper.countPendingByCompanyIdAndTaskId("c1", "t1")).thenReturn(0);

        DepartmentEntity department = new DepartmentEntity();
        department.setDepartmentId("d1");
        department.setCompanyId("c1");
        department.setManagerUserId("u-manager");
        when(departmentMapper.selectByPrimaryKey("d1")).thenReturn(department);

        OnboardingTaskDepartmentConfirmResponse response =
                (OnboardingTaskDepartmentConfirmResponse) processor.execute(context);

        assertEquals("CONFIRMED", response.getCheckpointStatus());
        assertEquals(OnboardingTaskWorkflow.STATUS_DONE, response.getTaskStatus());
        assertEquals(true, response.isAllDepartmentsConfirmed());
        verify(activityLogService).logStatusChanged(any(TaskInstanceEntity.class), any(TaskInstanceEntity.class), any(String.class));
        verify(progressService).recalculateFromTask("c1", task);
    }

    private static BizContext context(String operatorId) {
        BizContext context = new BizContext();
        context.setTenantId("c1");
        context.setOperatorId(operatorId);
        context.setRoles(Set.of("MANAGER"));
        return context;
    }

    private static TaskInstanceEntity task(String taskId) {
        TaskInstanceEntity task = new TaskInstanceEntity();
        task.setTaskId(taskId);
        task.setCompanyId("c1");
        task.setStatus(OnboardingTaskWorkflow.STATUS_TODO);
        task.setUpdatedAt(new Date());
        return task;
    }

    private static TaskDepartmentCheckpointEntity checkpoint(String taskId, String departmentId) {
        TaskDepartmentCheckpointEntity checkpoint = new TaskDepartmentCheckpointEntity();
        checkpoint.setTaskDepartmentCheckpointId("cp1");
        checkpoint.setCompanyId("c1");
        checkpoint.setTaskId(taskId);
        checkpoint.setDepartmentId(departmentId);
        checkpoint.setStatus("PENDING");
        checkpoint.setRequireEvidence(true);
        return checkpoint;
    }
}
