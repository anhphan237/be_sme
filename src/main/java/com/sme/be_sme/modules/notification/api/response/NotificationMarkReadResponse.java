package com.sme.be_sme.modules.notification.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationMarkReadResponse {
    /** Number of notifications marked as read */
    private int markedCount;
}
