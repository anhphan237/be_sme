package com.sme.be_sme.modules.onboarding.facade.impl;

import com.sme.be_sme.modules.onboarding.api.request.*;
import com.sme.be_sme.modules.onboarding.api.response.*;
import com.sme.be_sme.modules.onboarding.facade.OnboardingEventFacade;
import com.sme.be_sme.modules.onboarding.processor.*;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingEventFacadeImpl extends BaseOperationFacade implements OnboardingEventFacade {

    private final EventTemplateCreateProcessor eventTemplateCreateProcessor;
    private final EventTemplateDetailProcessor eventTemplateDetailProcessor;
    private final EventTemplateListProcessor eventTemplateListProcessor;
    private final EventPublishProcessor eventPublishProcessor;
    private final EventDetailProcessor eventDetailProcessor;
    private final EventInstanceListProcessor eventInstanceListProcessor;
    private final EventAttendanceSummaryProcessor eventAttendanceSummaryProcessor;
    private final EventAttendanceConfirmProcessor eventAttendanceConfirmProcessor;

    @Override
    public EventTemplateCreateResponse createEventTemplate(EventTemplateCreateRequest request) {
        return call(eventTemplateCreateProcessor, request, EventTemplateCreateResponse.class);
    }

    @Override
    public EventTemplateDetailResponse getEventTemplateDetail(EventTemplateDetailRequest request) {
        return call(eventTemplateDetailProcessor, request, EventTemplateDetailResponse.class);
    }

    @Override
    public EventTemplateListResponse listEventTemplates(EventTemplateListRequest request) {
        return call(eventTemplateListProcessor, request, EventTemplateListResponse.class);
    }

    @Override
    public EventPublishResponse publishEvent(EventPublishRequest request) {
        return call(eventPublishProcessor, request, EventPublishResponse.class);
    }

    @Override
    public EventDetailResponse getEventDetail(EventDetailRequest request) {
        return call(eventDetailProcessor, request, EventDetailResponse.class);
    }

    @Override
    public EventInstanceListResponse listEventInstances(EventInstanceListRequest request) {
        return call(eventInstanceListProcessor, request, EventInstanceListResponse.class);
    }

    @Override
    public EventAttendanceSummaryResponse summarizeEventAttendance(EventAttendanceSummaryRequest request) {
        return call(eventAttendanceSummaryProcessor, request, EventAttendanceSummaryResponse.class);
    }

    @Override
    public EventAttendanceConfirmResponse confirmEventAttendance(EventAttendanceConfirmRequest request) {
        return call(eventAttendanceConfirmProcessor, request, EventAttendanceConfirmResponse.class);
    }

}
