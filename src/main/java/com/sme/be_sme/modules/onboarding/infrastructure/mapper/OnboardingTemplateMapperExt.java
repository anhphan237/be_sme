package com.sme.be_sme.modules.onboarding.infrastructure.mapper;

import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.model.ChecklistTemplateRow;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.model.TaskTemplateRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OnboardingTemplateMapperExt {

    OnboardingTemplateEntity selectTemplateByIdAndCompany(
            @Param("templateId") String templateId,
            @Param("companyId") String companyId
    );

    List<ChecklistTemplateRow> selectChecklistRows(
            @Param("companyId") String companyId,
            @Param("templateId") String templateId
    );

    List<TaskTemplateRow> selectBaselineTaskRows(
            @Param("companyId") String companyId,
            @Param("templateId") String templateId
    );
}
