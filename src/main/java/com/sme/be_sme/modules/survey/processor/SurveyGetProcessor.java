<<<<<<< HEAD
=======
package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveyGetRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyGetResponse;
import com.sme.be_sme.modules.survey.api.response.SurveyTemplateResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyInstanceMapper;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyTemplateMapper;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyInstanceEntity;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyTemplateEntity;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SurveyGetProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SurveyInstanceMapper surveyInstanceMapper;
    private final SurveyTemplateMapper surveyTemplateMapper;
    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {

        List<SurveyInstanceEntity> instances = surveyInstanceMapper.selectAll();

        List<SurveyTemplateResponse> items = new ArrayList<>();

        for (SurveyInstanceEntity ins : instances) {

            SurveyTemplateEntity template =
                    surveyTemplateMapper.selectByPrimaryKey(ins.getSurveyTemplateId());

            if (template == null) continue;

            SurveyTemplateResponse dto = new SurveyTemplateResponse();
            dto.setTemplateId(template.getSurveyTemplateId());
            dto.setName(template.getName());
            dto.setStatus(ins.getStatus());

            items.add(dto);
        }

        SurveyGetResponse response = new SurveyGetResponse();
        response.setItems(items);
        return response;
    }

}
>>>>>>> 2548335 (add create surv question)
