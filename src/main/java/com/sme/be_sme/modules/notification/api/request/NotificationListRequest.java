package com.sme.be_sme.modules.notification.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationListRequest {
    /** If true, only return unread (read_at is null) */
    private Boolean unreadOnly;
    /** Page size, default 20 */
    private Integer limit = 20;
    /** Offset for pagination, default 0 */
    private Integer offset = 0;
}
