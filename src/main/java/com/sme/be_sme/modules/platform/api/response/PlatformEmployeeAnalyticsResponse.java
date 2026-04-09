package com.sme.be_sme.modules.platform.api.response;

import java.util.List;
import lombok.Data;

@Data
public class PlatformEmployeeAnalyticsResponse {
    private Integer totalEmployees;
    private Integer activeEmployees;
    private Integer newEmployeesInRange;
    private List<EmployeeByCompanyItem> employeesByCompany;
    private List<EmployeeByPlanItem> employeesByPlan;

    @Data
    public static class EmployeeByCompanyItem {
        private String companyId;
        private String companyName;
        private Integer employeeCount;
    }

    @Data
    public static class EmployeeByPlanItem {
        private String planId;
        private String planCode;
        private String planName;
        private Integer employeeCount;
    }
}
