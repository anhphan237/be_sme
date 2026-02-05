package com.sme.be_sme.modules.billing.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/**
 * Reads onboarding usage for billing limits. Queries onboarding_instances (same DB).
 */
@Mapper
public interface OnboardingUsageMapper {

    int countOnboardingInstancesByCompanyAndDateRange(
            @Param("companyId") String companyId,
            @Param("monthStart") Date monthStart,
            @Param("monthEnd") Date monthEnd
    );
}
