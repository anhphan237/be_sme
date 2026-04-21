package com.sme.be_sme.modules.platform.api.response;

import java.util.Date;
import java.util.List;

import lombok.Data;
@Data
public class PlatformErrorLogListResponse {

    private List<ErrorLogItem> items;
    private Integer total;
    private Integer page;
    private Integer size;

    @Data
    public static class ErrorLogItem {
        private String errorId;
        private String errorCode;
        private String message;
        private String stackTrace;
        private String requestId;
        private Date createdAt;

        private String operationType;
        private String tenantId;
        private String companyId;
        private String actorUserId;
        private String actorRole;
        private String severity;
        private String status;
        private String payloadSnapshot;

        private Date resolvedAt;
        private String resolvedBy;
        private String resolutionNote;
    }
}