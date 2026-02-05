package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveyTemplateGetListRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyTemplateListResponse;
import com.sme.be_sme.modules.survey.api.response.SurveyTemplateResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyTemplateMapper;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyTemplateEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SurveyTemplateGetListProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SurveyTemplateMapper surveyTemplateMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        SurveyTemplateGetListRequest request =
                payload == null ? new SurveyTemplateGetListRequest()
                        : objectMapper.convertValue(payload, SurveyTemplateGetListRequest.class);

        List<SurveyTemplateEntity> all = surveyTemplateMapper.selectAll();

        List<SurveyTemplateResponse> items = new ArrayList<>();
        for (SurveyTemplateEntity t : all) {

            if (t == null || !context.getTenantId().equals(t.getCompanyId())) continue;

            if (StringUtils.hasText(request.getStatus()) && !request.getStatus().equals(t.getStatus())) continue;
            if (StringUtils.hasText(request.getStage()) && !request.getStage().equals(t.getStage())) continue;
            if (request.getManagerOnly() != null && !request.getManagerOnly().equals(t.getManagerOnly())) continue;

            SurveyTemplateResponse dto = new SurveyTemplateResponse();
            dto.setTemplateId(t.getSurveyTemplateId());
            dto.setName(t.getName());
            dto.setStatus(t.getStatus());
            dto.setDescription(t.getDescription());
            dto.setStage(t.getStage());
            dto.setManagerOnly(t.getManagerOnly());
            dto.setVersion(t.getVersion());
            dto.setCreatedBy(t.getCreatedBy());
            dto.setCreatedAt(t.getCreatedAt());
            dto.setUpdatedAt(t.getUpdatedAt());
            dto.setIsDefault(t.getIsDefault());
            items.add(dto);
        }

        SurveyTemplateListResponse res = new SurveyTemplateListResponse();
        res.setItems(items);
        return res;
    }
}
