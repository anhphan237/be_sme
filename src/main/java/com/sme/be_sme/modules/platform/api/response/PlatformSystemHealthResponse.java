package com.sme.be_sme.modules.platform.api.response;

import lombok.Data;

@Data
public class PlatformSystemHealthResponse {
    private String dbStatus;
    private int activeConnections;
    private int totalCompanies;
    private int totalUsers;
    private String uptime;
}
