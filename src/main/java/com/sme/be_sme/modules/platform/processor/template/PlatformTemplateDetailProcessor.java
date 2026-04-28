package com.sme.be_sme.modules.platform.processor.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.platform.api.request.PlatformTemplateDetailRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformTemplateDetailChecklistResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformTemplateDetailResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformTemplateDetailTaskResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformTemplateRequiredDocumentResponse;
import com.sme.be_sme.modules.platform.infrastructure.mapper.PlatformTemplateMapperExt;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PlatformTemplateDetailProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final PlatformTemplateMapperExt platformTemplateMapperExt;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformTemplateBizHelper.assertPlatformAdmin(context, "detail");

        PlatformTemplateDetailRequest request =
                objectMapper.convertValue(payload, PlatformTemplateDetailRequest.class);

        String templateId = PlatformTemplateBizHelper.requireTemplateId(
                request == null ? null : request.getTemplateId());

        PlatformTemplateDetailResponse response =
                platformTemplateMapperExt.selectPlatformTemplateDetail(
                        PlatformTemplateBizHelper.PLATFORM_COMPANY_ID,
                        templateId
                );

        if (response == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "platform onboarding template not found");
        }

        List<PlatformTemplateDetailChecklistResponse> checklists =
                platformTemplateMapperExt.selectTemplateChecklists(
                        PlatformTemplateBizHelper.PLATFORM_COMPANY_ID,
                        templateId
                );

        List<PlatformTemplateDetailTaskResponse> tasks =
                platformTemplateMapperExt.selectTemplateTasks(
                        PlatformTemplateBizHelper.PLATFORM_COMPANY_ID,
                        templateId
                );

        List<PlatformTemplateRequiredDocumentResponse> requiredDocuments =
                platformTemplateMapperExt.selectRequiredDocumentsByTemplateId(
                        PlatformTemplateBizHelper.PLATFORM_COMPANY_ID,
                        templateId
                );

        Map<String, List<String>> requiredDocumentMap =
                requiredDocuments.stream()
                        .collect(Collectors.groupingBy(
                                PlatformTemplateRequiredDocumentResponse::getTaskTemplateId,
                                Collectors.mapping(
                                        PlatformTemplateRequiredDocumentResponse::getDocumentId,
                                        Collectors.toList()
                                )
                        ));

        for (PlatformTemplateDetailTaskResponse task : tasks) {
            task.setRequiredDocumentIds(
                    requiredDocumentMap.getOrDefault(task.getTaskTemplateId(), List.of())
            );
        }

        Map<String, List<PlatformTemplateDetailTaskResponse>> taskMap =
                tasks.stream()
                        .collect(Collectors.groupingBy(
                                PlatformTemplateDetailTaskResponse::getChecklistTemplateId
                        ));

        for (PlatformTemplateDetailChecklistResponse checklist : checklists) {
            checklist.setTasks(
                    taskMap.getOrDefault(checklist.getChecklistTemplateId(), List.of())
            );
        }

        response.setChecklists(checklists);
        return response;
    }
}