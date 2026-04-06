package com.sme.be_sme.modules.platform.api.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class PlatformAdminAuditLogResponse {
    private List<AdminAuditLogItem> items;
    private Integer total;

    @Data
    public static class AdminAuditLogItem {
        private String logId;
        private String adminUserId;
        private String action;
        private String targetType;
        private String targetId;
        private String detail;
        private Date createdAt;
    }
}