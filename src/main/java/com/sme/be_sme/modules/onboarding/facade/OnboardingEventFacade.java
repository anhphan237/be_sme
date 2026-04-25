package com.sme.be_sme.modules.onboarding.facade;

import com.sme.be_sme.modules.onboarding.api.request.EventDetailRequest;
import com.sme.be_sme.modules.onboarding.api.request.EventInstanceListRequest;
import com.sme.be_sme.modules.onboarding.api.request.EventPublishRequest;
import com.sme.be_sme.modules.onboarding.api.request.EventTemplateCreateRequest;
import com.sme.be_sme.modules.onboarding.api.response.EventDetailResponse;
import com.sme.be_sme.modules.onboarding.api.response.EventInstanceListResponse;
import com.sme.be_sme.modules.onboarding.api.response.EventPublishResponse;
import com.sme.be_sme.modules.onboarding.api.response.EventTemplateCreateResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface OnboardingEventFacade extends OperationFacadeProvider {

    @OperationType("com.sme.onboarding.eventTemplate.create")
    EventTemplateCreateResponse createEventTemplate(EventTemplateCreateRequest request);

    @OperationType("com.sme.onboarding.event.publish")
    EventPublishResponse publishEvent(EventPublishRequest request);

    @OperationType("com.sme.onboarding.event.detail")
    EventDetailResponse getEventDetail(EventDetailRequest request);

    @OperationType("com.sme.onboarding.event.list")
    EventInstanceListResponse listEventInstances(EventInstanceListRequest request);
}
