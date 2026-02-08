package com.sme.be_sme.modules.notification.api.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NotificationMarkReadRequest {
    /** Notification IDs to mark as read */
    private List<String> notificationIds;
}
