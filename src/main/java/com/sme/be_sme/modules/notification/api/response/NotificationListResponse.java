package com.sme.be_sme.modules.notification.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class NotificationListResponse {
    private List<NotificationItem> items;
    private int totalCount;

    @Getter
    @Setter
    public static class NotificationItem {
        private String notificationId;
        private String type;
        private String title;
        private String content;
        private String status;
        private Date readAt;
        private Date createdAt;
        private String refType;
        private String refId;
    }
}
