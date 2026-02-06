package com.sme.be_sme.modules.survey.infrastructure.mapper;

import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyAnswerEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SurveyAnswerMapperExt {

    /**
     * Select answers by company and response IDs (for satisfaction aggregation).
     */
    List<SurveyAnswerEntity> selectByCompanyIdAndResponseIds(
            @Param("companyId") String companyId,
            @Param("responseIds") List<String> responseIds
    );
}
