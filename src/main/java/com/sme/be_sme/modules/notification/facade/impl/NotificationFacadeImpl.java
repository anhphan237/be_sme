package com.sme.be_sme.modules.notification.facade.impl;

import com.sme.be_sme.modules.notification.api.request.NotificationListRequest;
import com.sme.be_sme.modules.notification.api.request.NotificationMarkReadRequest;
import com.sme.be_sme.modules.notification.api.response.NotificationListResponse;
import com.sme.be_sme.modules.notification.api.response.NotificationMarkReadResponse;
import com.sme.be_sme.modules.notification.facade.NotificationFacade;
import com.sme.be_sme.modules.notification.processor.NotificationListProcessor;
import com.sme.be_sme.modules.notification.processor.NotificationMarkReadProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationFacadeImpl extends BaseOperationFacade implements NotificationFacade {

    private final NotificationListProcessor notificationListProcessor;
    private final NotificationMarkReadProcessor notificationMarkReadProcessor;

    @Override
    public NotificationListResponse listNotifications(NotificationListRequest request) {
        return call(notificationListProcessor, request, NotificationListResponse.class);
    }

    @Override
    public NotificationMarkReadResponse markRead(NotificationMarkReadRequest request) {
        return call(notificationMarkReadProcessor, request, NotificationMarkReadResponse.class);
    }
}
