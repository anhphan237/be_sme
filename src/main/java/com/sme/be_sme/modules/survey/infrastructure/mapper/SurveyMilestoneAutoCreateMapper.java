package com.sme.be_sme.modules.survey.infrastructure.mapper;

import com.sme.be_sme.modules.survey.infrastructure.persistence.model.SurveyMilestoneCandidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface SurveyMilestoneAutoCreateMapper {
    List<SurveyMilestoneCandidate> selectDefaultMilestoneCandidates(
            @Param("now") Date now,
            @Param("limit") int limit
    );

    int insertAutoSentSurveyInstance(SurveyMilestoneCandidate candidate);
}
