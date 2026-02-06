package com.sme.be_sme.modules.notification.facade;

import com.sme.be_sme.modules.notification.api.request.NotificationListRequest;
import com.sme.be_sme.modules.notification.api.request.NotificationMarkReadRequest;
import com.sme.be_sme.modules.notification.api.response.NotificationListResponse;
import com.sme.be_sme.modules.notification.api.response.NotificationMarkReadResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface NotificationFacade extends OperationFacadeProvider {

    @OperationType("com.sme.notification.list")
    NotificationListResponse listNotifications(NotificationListRequest request);

    @OperationType("com.sme.notification.markRead")
    NotificationMarkReadResponse markRead(NotificationMarkReadRequest request);
}
