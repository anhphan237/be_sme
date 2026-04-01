package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapperExt;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapper;
import com.sme.be_sme.modules.onboarding.api.response.TaskTimelineByOnboardingResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapperExt;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskAssigneeListRow;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskTimelineByOnboardingProcessorTest {

    @Mock
    private OnboardingInstanceMapper onboardingInstanceMapper;
    @Mock
    private TaskInstanceMapperExt taskInstanceMapperExt;
    @Mock
    private UserMapper userMapper;
    @Mock
    private EmployeeProfileMapperExt employeeProfileMapperExt;

    @Test
    void timeline_groupsByAssignee_andDefaultsToExcludeDone() {
        TaskTimelineByOnboardingProcessor processor = new TaskTimelineByOnboardingProcessor(
                new ObjectMapper(),
                onboardingInstanceMapper,
                taskInstanceMapperExt,
                userMapper,
                employeeProfileMapperExt
        );
        BizContext context = new BizContext();
        context.setTenantId("c1");
        context.setOperatorId("hr1");
        context.setRoles(Set.of("HR"));
        context.setPayload(new ObjectMapper().createObjectNode().put("onboardingId", "ob1"));

        OnboardingInstanceEntity instance = new OnboardingInstanceEntity();
        instance.setOnboardingId("ob1");
        instance.setCompanyId("c1");
        when(onboardingInstanceMapper.selectByPrimaryKey("ob1")).thenReturn(instance);

        TaskAssigneeListRow task1 = new TaskAssigneeListRow();
        task1.setTaskId("t1");
        task1.setAssignedUserId("u1");
        task1.setTitle("Task 1");
        TaskAssigneeListRow task2 = new TaskAssigneeListRow();
        task2.setTaskId("t2");
        task2.setAssignedUserId("u2");
        task2.setTitle("Task 2");
        when(taskInstanceMapperExt.selectTimelineByOnboardingId("c1", "ob1", false))
                .thenReturn(List.of(task1, task2));

        TaskTimelineByOnboardingResponse response =
                (TaskTimelineByOnboardingResponse) processor.execute(context);

        assertEquals("ob1", response.getOnboardingId());
        assertEquals(2, response.getTotalTasks());
        assertEquals(2, response.getAssignees().size());
        verify(taskInstanceMapperExt).selectTimelineByOnboardingId("c1", "ob1", false);
    }
}

