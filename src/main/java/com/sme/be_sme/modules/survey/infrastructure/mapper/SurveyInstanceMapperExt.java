package com.sme.be_sme.modules.survey.infrastructure.mapper;

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
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    int countByCompanyId(
            @Param("companyId") String companyId,
            @Param("templateId") String templateId,
            @Param("status") String status,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );

    @Select("""
        select count(*) 
        from survey_instances
        where company_id = #{companyId}
          and (#{templateId} is null or survey_template_id = #{templateId})
          and (#{startDate} is null or created_at >= #{startDate})
          and (#{endDate} is null or created_at <= #{endDate})
    """)
    int countSent(
            @Param("companyId") String companyId,
            @Param("templateId") String templateId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );
}
