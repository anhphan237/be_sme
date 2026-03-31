package com.sme.be_sme.modules.onboarding.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateAIGenerateResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.ChecklistTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.ChecklistTemplateEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskTemplateEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.util.UuidGenerator;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingTemplateGeneratorService {

    private final ObjectMapper objectMapper;
    private final ChatLanguageModel chatModel;
    private final OnboardingTemplateMapper onboardingTemplateMapper;
    private final ChecklistTemplateMapper checklistTemplateMapper;
    private final TaskTemplateMapper taskTemplateMapper;

    private static final String PROMPT_TEMPLATE =
            "Bạn là chuyên gia HR tại Việt Nam. Hãy tạo quy trình onboarding cho công ty có thông tin sau:\n"
            + "- Ngành nghề: %s\n"
            + "- Quy mô: %s\n"
            + "- Vị trí nhân viên mới: %s\n\n"
            + "Trả về JSON (CHỈ JSON thuần, không có markdown, không có ```json, không có text thêm) với cấu trúc:\n"
            + "{\n"
            + "  \"name\": \"Tên template\",\n"
            + "  \"description\": \"Mô tả ngắn về quy trình onboarding\",\n"
            + "  \"checklists\": [\n"
            + "    {\n"
            + "      \"name\": \"Tên giai đoạn\",\n"
            + "      \"stage\": \"PRE\",\n"
            + "      \"sortOrder\": 1,\n"
            + "      \"tasks\": [\n"
            + "        {\n"
            + "          \"title\": \"Tên công việc\",\n"
            + "          \"description\": \"Mô tả chi tiết công việc\",\n"
            + "          \"ownerType\": \"HR\",\n"
            + "          \"dueDaysOffset\": -3,\n"
            + "          \"requireAck\": false,\n"
            + "          \"sortOrder\": 1\n"
            + "        }\n"
            + "      ]\n"
            + "    }\n"
            + "  ]\n"
            + "}\n\n"
            + "Quy tắc:\n"
            + "- stage chỉ dùng: PRE (trước ngày đầu, dueDaysOffset âm), DAY1 (ngày 1), D7 (tuần đầu), D30 (tháng đầu), D60 (tháng thứ 2)\n"
            + "- ownerType chỉ dùng: HR, MANAGER, EMPLOYEE, IT_STAFF\n"
            + "- Tạo 4-6 giai đoạn (checklists) với tổng 15-25 tasks, phù hợp với ngành và vị trí\n"
            + "- Trả lời hoàn toàn bằng tiếng Việt (tên, mô tả)\n"
            + "- QUAN TRỌNG: Chỉ trả về JSON thuần, không có bất kỳ text nào khác";

    @Transactional(rollbackFor = Exception.class)
    public OnboardingTemplateAIGenerateResponse generate(String companyId, String createdBy,
                                                          String industry, String companySize, String jobRole) {
        String safeIndustry = StringUtils.hasText(industry) ? industry.trim() : "Chưa xác định";
        String safeSize = StringUtils.hasText(companySize) ? companySize.trim() : "SME";
        String safeRole = StringUtils.hasText(jobRole) ? jobRole.trim() : "Nhân viên mới";

        String prompt = String.format(PROMPT_TEMPLATE, safeIndustry, safeSize, safeRole);

        String aiResponse;
        try {
            aiResponse = chatModel.generate(prompt);
        } catch (Exception e) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "AI service failed: " + e.getMessage());
        }

        JsonNode aiJson = parseAiResponse(aiResponse);

        String templateId = UuidGenerator.generate();
        Date now = new Date();

        OnboardingTemplateEntity templateEntity = new OnboardingTemplateEntity();
        templateEntity.setOnboardingTemplateId(templateId);
        templateEntity.setCompanyId(companyId);
        templateEntity.setName(aiJson.path("name").asText("Quy trình onboarding AI"));
        templateEntity.setDescription(aiJson.path("description").asText(null));
        templateEntity.setStatus("DRAFT");
        templateEntity.setCreatedBy(StringUtils.hasText(createdBy) ? createdBy : "system");
        templateEntity.setCreatedAt(now);
        templateEntity.setUpdatedAt(now);

        if (onboardingTemplateMapper.insert(templateEntity) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "Failed to save AI-generated onboarding template");
        }

        int totalChecklists = 0;
        int totalTasks = 0;

        JsonNode checklists = aiJson.path("checklists");
        if (checklists.isArray()) {
            int checklistOrder = 0;
            for (JsonNode checklistNode : checklists) {
                String checklistId = UuidGenerator.generate();
                ChecklistTemplateEntity checklistEntity = new ChecklistTemplateEntity();
                checklistEntity.setChecklistTemplateId(checklistId);
                checklistEntity.setCompanyId(companyId);
                checklistEntity.setOnboardingTemplateId(templateId);
                checklistEntity.setName(checklistNode.path("name").asText("Giai đoạn"));
                checklistEntity.setStage(checklistNode.path("stage").asText("DAY1"));
                checklistEntity.setSortOrder(checklistNode.path("sortOrder").asInt(checklistOrder));
                checklistEntity.setStatus("ACTIVE");
                checklistEntity.setCreatedAt(now);
                checklistEntity.setUpdatedAt(now);

                if (checklistTemplateMapper.insert(checklistEntity) != 1) {
                    throw AppException.of(ErrorCodes.INTERNAL_ERROR, "Failed to save AI-generated checklist");
                }
                totalChecklists++;
                checklistOrder++;

                JsonNode tasks = checklistNode.path("tasks");
                if (tasks.isArray()) {
                    int taskOrder = 0;
                    for (JsonNode taskNode : tasks) {
                        String title = taskNode.path("title").asText(null);
                        if (!StringUtils.hasText(title)) continue;

                        TaskTemplateEntity taskEntity = new TaskTemplateEntity();
                        taskEntity.setTaskTemplateId(UuidGenerator.generate());
                        taskEntity.setCompanyId(companyId);
                        taskEntity.setChecklistTemplateId(checklistId);
                        taskEntity.setTitle(title.trim());
                        taskEntity.setDescription(taskNode.path("description").asText(null));
                        taskEntity.setOwnerType(taskNode.path("ownerType").asText("HR"));
                        taskEntity.setOwnerRefId(null);
                        taskEntity.setDueDaysOffset(taskNode.path("dueDaysOffset").asInt(0));
                        taskEntity.setRequireAck(taskNode.path("requireAck").asBoolean(false));
                        taskEntity.setSortOrder(taskNode.path("sortOrder").asInt(taskOrder));
                        taskEntity.setStatus("ACTIVE");
                        taskEntity.setCreatedAt(now);
                        taskEntity.setUpdatedAt(now);

                        if (taskTemplateMapper.insert(taskEntity) != 1) {
                            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "Failed to save AI-generated task");
                        }
                        totalTasks++;
                        taskOrder++;
                    }
                }
            }
        }

        OnboardingTemplateAIGenerateResponse response = new OnboardingTemplateAIGenerateResponse();
        response.setTemplateId(templateId);
        response.setName(templateEntity.getName());
        response.setTotalChecklists(totalChecklists);
        response.setTotalTasks(totalTasks);
        return response;
    }

    private JsonNode parseAiResponse(String aiResponse) {
        String cleaned = aiResponse.trim();
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline >= 0) cleaned = cleaned.substring(firstNewline + 1);
            if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.lastIndexOf("```")).trim();
        }
        try {
            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            log.error("Failed to parse AI response as JSON: {}", cleaned, e);
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "AI returned invalid JSON. Please try again.");
        }
    }
}
