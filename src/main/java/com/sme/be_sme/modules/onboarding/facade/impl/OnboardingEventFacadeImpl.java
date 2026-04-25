package com.sme.be_sme.modules.onboarding.facade.impl;

import com.sme.be_sme.modules.onboarding.api.request.EventDetailRequest;
import com.sme.be_sme.modules.onboarding.api.request.EventInstanceListRequest;
import com.sme.be_sme.modules.onboarding.api.request.EventPublishRequest;
import com.sme.be_sme.modules.onboarding.api.request.EventTemplateCreateRequest;
import com.sme.be_sme.modules.onboarding.api.response.EventDetailResponse;
import com.sme.be_sme.modules.onboarding.api.response.EventInstanceListResponse;
import com.sme.be_sme.modules.onboarding.api.response.EventPublishResponse;
import com.sme.be_sme.modules.onboarding.api.response.EventTemplateCreateResponse;
import com.sme.be_sme.modules.onboarding.facade.OnboardingEventFacade;
import com.sme.be_sme.modules.onboarding.processor.EventDetailProcessor;
import com.sme.be_sme.modules.onboarding.processor.EventInstanceListProcessor;
import com.sme.be_sme.modules.onboarding.processor.EventPublishProcessor;
import com.sme.be_sme.modules.onboarding.processor.EventTemplateCreateProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingEventFacadeImpl extends BaseOperationFacade implements OnboardingEventFacade {

    private final EventTemplateCreateProcessor eventTemplateCreateProcessor;
    private final EventPublishProcessor eventPublishProcessor;
    private final EventDetailProcessor eventDetailProcessor;
    private final EventInstanceListProcessor eventInstanceListProcessor;

    @Override
    public EventTemplateCreateResponse createEventTemplate(EventTemplateCreateRequest request) {
        return call(eventTemplateCreateProcessor, request, EventTemplateCreateResponse.class);
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
}
