package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.TaskScheduleCalendarRequest;
import com.sme.be_sme.modules.onboarding.api.response.TaskScheduleCalendarResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapperExt;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskAssigneeListRow;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskAuth;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class TaskScheduleCalendarProcessor extends BaseBizProcessor<BizContext> {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ObjectMapper objectMapper;
    private final TaskInstanceMapperExt taskInstanceMapperExt;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        TaskScheduleCalendarRequest request =
                payload == null || payload.isNull() || payload.isEmpty()
                        ? new TaskScheduleCalendarRequest()
                        : objectMapper.convertValue(payload, TaskScheduleCalendarRequest.class);

        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (!StringUtils.hasText(context.getOperatorId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "operatorId is required");
        }

        if (request.getFromTime() != null && request.getToTime() != null
                && request.getFromTime().after(request.getToTime())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "fromTime must be before or equal to toTime");
        }

        int page = request.getPage() != null && request.getPage() > 0 ? request.getPage() : DEFAULT_PAGE;
        int size = request.getSize() != null && request.getSize() > 0 ? Math.min(request.getSize(), MAX_SIZE) : DEFAULT_SIZE;
        int offset = (page - 1) * size;

        String companyId = context.getTenantId().trim();
        String operatorId = context.getOperatorId().trim();
        String targetUserId = StringUtils.hasText(request.getUserId()) ? request.getUserId().trim() : operatorId;
        boolean selfView = operatorId.equals(targetUserId);
        boolean fullView = selfView || OnboardingTaskAuth.isHrManagerAdmin(context.getRoles());

        List<TaskAssigneeListRow> rows = taskInstanceMapperExt.selectScheduledCalendarByCompanyAndUser(
                companyId,
                targetUserId,
                request.getFromTime(),
                request.getToTime(),
                offset,
                size);
        Integer totalObj = taskInstanceMapperExt.countScheduledCalendarByCompanyAndUser(
                companyId,
                targetUserId,
                request.getFromTime(),
                request.getToTime());
        int total = totalObj == null ? 0 : totalObj;

        TaskScheduleCalendarResponse response = new TaskScheduleCalendarResponse();
        response.setTargetUserId(targetUserId);
        response.setSelfView(selfView);
        response.setTotalCount(total);
        response.setPage(page);
        response.setSize(size);
        response.setItems(rows.stream().map(row -> toItem(row, fullView)).toList());
        return response;
    }

    private static TaskScheduleCalendarResponse.CalendarItem toItem(TaskAssigneeListRow row, boolean fullView) {
        TaskScheduleCalendarResponse.CalendarItem item = new TaskScheduleCalendarResponse.CalendarItem();
        item.setScheduledStartAt(row.getScheduledStartAt());
        item.setScheduledEndAt(row.getScheduledEndAt());
        if (fullView) {
            item.setTaskId(row.getTaskId());
            item.setTitle(row.getTitle());
            item.setStatus(row.getStatus());
            item.setOnboardingId(row.getOnboardingId());
            item.setChecklistName(row.getChecklistName());
        }
        return item;
    }
}
