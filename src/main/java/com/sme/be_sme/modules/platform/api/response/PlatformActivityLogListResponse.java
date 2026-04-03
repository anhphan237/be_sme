package com.sme.be_sme.modules.platform.api.response;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class PlatformActivityLogListResponse {
    private List<ActivityLogItem> items;
    private int total;

    @Data
    public static class ActivityLogItem {
        private String logId;
        private String companyId;
        private String userId;
        private String action;
        private String entityType;
        private String entityId;
        private String detail;
        private Date createdAt;
    }
}
