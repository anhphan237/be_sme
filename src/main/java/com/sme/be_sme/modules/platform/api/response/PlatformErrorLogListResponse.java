package com.sme.be_sme.modules.platform.api.response;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class PlatformErrorLogListResponse {
    private List<ErrorLogItem> items;
    private int total;

    @Data
    public static class ErrorLogItem {
        private String errorId;
        private String errorCode;
        private String message;
        private String requestId;
        private Date createdAt;
    }
}
