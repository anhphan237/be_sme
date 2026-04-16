package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskAttachmentMapperExt;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskRequiredDocumentMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.ChecklistInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentAcknowledgementMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.service.OnboardingInstanceProgressService;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskActivityLogService;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskApprovalAuthority;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskWorkflowNotificationService;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskWorkflow;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingTaskUpdateStatusProcessorTest {

    @Mock
    private TaskInstanceMapper taskInstanceMapper;
    @Mock
    private TaskAttachmentMapperExt taskAttachmentMapperExt;
    @Mock
    private TaskRequiredDocumentMapper taskRequiredDocumentMapper;
    @Mock
    private ChecklistInstanceMapper checklistInstanceMapper;
    @Mock
    private DocumentAcknowledgementMapper documentAcknowledgementMapper;
    @Mock
    private OnboardingInstanceProgressService progressService;
    @Mock
    private OnboardingTaskApprovalAuthority approvalAuthority;
    @Mock
    private OnboardingTaskActivityLogService activityLogService;
    @Mock
    private OnboardingTaskWorkflowNotificationService workflowNotificationService;

    @Test
    void updateToPendingApproval_logsAndNotifiesApprover() {
        OnboardingTaskUpdateStatusProcessor processor = new OnboardingTaskUpdateStatusProcessor(
                new ObjectMapper(),
                taskInstanceMapper,
                taskAttachmentMapperExt,
                taskRequiredDocumentMapper,
                checklistInstanceMapper,
                documentAcknowledgementMapper,
                progressService,
                approvalAuthority,
                activityLogService,
                workflowNotificationService
        );
        BizContext context = new BizContext();
        context.setTenantId("c1");
        context.setOperatorId("u1");
        context.setRoles(Set.of("EMPLOYEE"));
        context.setPayload(new ObjectMapper().createObjectNode()
                .put("taskId", "t1")
                .put("status", "PENDING_APPROVAL"));

        TaskInstanceEntity task = new TaskInstanceEntity();
        task.setTaskId("t1");
        task.setCompanyId("c1");
        task.setAssignedUserId("u1");
        task.setStatus(OnboardingTaskWorkflow.STATUS_TODO);
        task.setRequiresManagerApproval(true);

        when(taskInstanceMapper.selectByPrimaryKey("t1")).thenReturn(task);
        when(taskInstanceMapper.updateByPrimaryKey(any(TaskInstanceEntity.class))).thenReturn(1);

        OnboardingTaskResponse response = (OnboardingTaskResponse) processor.execute(context);
        assertEquals(OnboardingTaskWorkflow.STATUS_PENDING_APPROVAL, response.getStatus());
        verify(activityLogService).logStatusChanged(any(TaskInstanceEntity.class), any(TaskInstanceEntity.class), any(String.class));
        verify(workflowNotificationService).notifyPendingApproval(any(TaskInstanceEntity.class), any(String.class));
        verify(progressService).recalculateFromTask("c1", task);
    }

    @Test
    void done_rejectedWhenScheduleNotConfirmed() {
        OnboardingTaskUpdateStatusProcessor processor = new OnboardingTaskUpdateStatusProcessor(
                new ObjectMapper(),
                taskInstanceMapper,
                taskAttachmentMapperExt,
                taskRequiredDocumentMapper,
                checklistInstanceMapper,
                documentAcknowledgementMapper,
                progressService,
                approvalAuthority,
                activityLogService,
                workflowNotificationService
        );
        BizContext context = new BizContext();
        context.setTenantId("c1");
        context.setOperatorId("u1");
        context.setRoles(Set.of("EMPLOYEE"));
        context.setPayload(new ObjectMapper().createObjectNode()
                .put("taskId", "t1")
                .put("status", "DONE"));

        TaskInstanceEntity task = new TaskInstanceEntity();
        task.setTaskId("t1");
        task.setCompanyId("c1");
        task.setAssignedUserId("u1");
        task.setStatus(OnboardingTaskWorkflow.STATUS_TODO);
        task.setScheduleStatus(OnboardingTaskWorkflow.SCHEDULE_PROPOSED);
        when(taskInstanceMapper.selectByPrimaryKey("t1")).thenReturn(task);

        assertThrows(AppException.class, () -> processor.execute(context));
    }

    @Test
    void pendingApproval_rejectedWhenRequiredDocMissing() {
        OnboardingTaskUpdateStatusProcessor processor = new OnboardingTaskUpdateStatusProcessor(
                new ObjectMapper(),
                taskInstanceMapper,
                taskAttachmentMapperExt,
                taskRequiredDocumentMapper,
                checklistInstanceMapper,
                documentAcknowledgementMapper,
                progressService,
                approvalAuthority,
                activityLogService,
                workflowNotificationService
        );
        BizContext context = new BizContext();
        context.setTenantId("c1");
        context.setOperatorId("u1");
        context.setRoles(Set.of("EMPLOYEE"));
        context.setPayload(new ObjectMapper().createObjectNode()
                .put("taskId", "t1")
                .put("status", "PENDING_APPROVAL"));

        TaskInstanceEntity task = new TaskInstanceEntity();
        task.setTaskId("t1");
        task.setCompanyId("c1");
        task.setAssignedUserId("u1");
        task.setStatus(OnboardingTaskWorkflow.STATUS_TODO);
        task.setRequiresManagerApproval(true);
        task.setRequireDoc(true);

        when(taskInstanceMapper.selectByPrimaryKey("t1")).thenReturn(task);
        when(taskAttachmentMapperExt.selectByTaskId(eq("c1"), eq("t1"))).thenReturn(java.util.List.of());

        assertThrows(AppException.class, () -> processor.execute(context));
    }
}

