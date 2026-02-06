package com.sme.be_sme.modules.onboarding.infrastructure.mapper;

import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface OnboardingInstanceMapperExt {

    /**
     * Instances with start_date = the given date and status = ACTIVE (for pre-first-day email).
     */
    List<OnboardingInstanceEntity> selectByStartDateAndActive(@Param("startDate") Date startDate);
}
