package com.sme.be_sme.modules.survey.infrastructure.mapper;

import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyInstanceEntity;
import com.sme.be_sme.modules.survey.infrastructure.persistence.model.SurveyInstanceDetailRow;
import com.sme.be_sme.modules.survey.infrastructure.persistence.model.SurveyInstanceListRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

@Mapper
public interface SurveyInstanceMapperExt {

    List<SurveyInstanceListRow> selectListByCompanyId(
            @Param("companyId") String companyId,
            @Param("templateId") String templateId,
            @Param("status") String status,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate,
            @Param("responderUserId") String responderUserId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    int countByCompanyId(
            @Param("companyId") String companyId,
            @Param("templateId") String templateId,
            @Param("status") String status,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate,
            @Param("responderUserId") String responderUserId
    );

    @Select("""
    select count(*)
    from survey_instances
    where company_id = #{companyId}
      and (#{templateId} is null or survey_template_id = #{templateId})
      and status in ('SENT', 'SCHEDULED', 'COMPLETED', 'EXPIRED')
      and (#{startDate} is null or coalesce(sent_at, scheduled_at, created_at) >= #{startDate})
      and (#{endDate} is null or coalesce(sent_at, scheduled_at, created_at) <= #{endDate})
""")
    int countSent(
            @Param("companyId") String companyId,
            @Param("templateId") String templateId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );
    SurveyInstanceDetailRow selectDetailById(
            @Param("companyId") String companyId,
            @Param("instanceId") String instanceId,
            @Param("responderUserId") String responderUserId
    );
    SurveyInstanceEntity findActiveByUniqueKey(
            @Param("companyId") String companyId,
            @Param("onboardingId") String onboardingId,
            @Param("templateId") String templateId,
            @Param("responderUserId") String responderUserId
    );

}
