package com.sme.be_sme.modules.onboarding.facade;

import com.sme.be_sme.modules.onboarding.api.request.*;
import com.sme.be_sme.modules.onboarding.api.response.*;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface OnboardingEventFacade extends OperationFacadeProvider {

    @OperationType("com.sme.onboarding.eventTemplate.create")
    EventTemplateCreateResponse createEventTemplate(EventTemplateCreateRequest request);

    @OperationType("com.sme.onboarding.eventTemplate.detail")
    EventTemplateDetailResponse getEventTemplateDetail(EventTemplateDetailRequest request);

    @OperationType("com.sme.onboarding.eventTemplate.list")
    EventTemplateListResponse listEventTemplates(EventTemplateListRequest request);

    @OperationType("com.sme.onboarding.event.publish")
    EventPublishResponse publishEvent(EventPublishRequest request);

    @OperationType("com.sme.onboarding.event.detail")
    EventDetailResponse getEventDetail(EventDetailRequest request);

    @OperationType("com.sme.onboarding.event.list")
    EventInstanceListResponse listEventInstances(EventInstanceListRequest request);

    @OperationType("com.sme.onboarding.event.attendance.summary")
    EventAttendanceSummaryResponse summarizeEventAttendance(EventAttendanceSummaryRequest request);

    @OperationType("com.sme.onboarding.event.attendance.confirm")
    EventAttendanceConfirmResponse confirmEventAttendance(EventAttendanceConfirmRequest request);
    @OperationType("com.sme.onboarding.event.complete")
    EventCompleteResponse completeEvent(EventCompleteRequest request);
}
