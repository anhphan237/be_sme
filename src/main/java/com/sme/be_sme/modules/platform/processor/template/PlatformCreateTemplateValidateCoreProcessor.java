package com.sme.be_sme.modules.platform.processor.template;

import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.modules.onboarding.api.request.ChecklistTemplateCreateItem;
import com.sme.be_sme.modules.onboarding.api.request.TaskTemplateCreateItem;
import com.sme.be_sme.modules.platform.context.PlatformCreateTemplateContext;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PlatformCreateTemplateValidateCoreProcessor extends BaseCoreProcessor<PlatformCreateTemplateContext> {

    private final DocumentMapper documentMapper;

    @Override
    protected Object process(PlatformCreateTemplateContext ctx) {
        if (ctx.getRequest() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "request is required");
        }

        if (!StringUtils.hasText(ctx.getRequest().getName())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "name is required");
        }

        if (CollectionUtils.isEmpty(ctx.getRequest().getChecklists())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "checklists is required");
        }

        String companyId = ctx.getCompanyId();

        for (ChecklistTemplateCreateItem checklist : ctx.getRequest().getChecklists()) {
            validateChecklist(companyId, checklist);
        }

        return null;
    }

    private void validateChecklist(String companyId, ChecklistTemplateCreateItem checklist) {
        if (checklist == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "checklist item is required");
        }

        if (!StringUtils.hasText(checklist.getName())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "checklist name is required");
        }

        if (!StringUtils.hasText(checklist.getStage())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "checklist stage is required");
        }

        if (checklist.getDeadlineDays() != null && checklist.getDeadlineDays() < 0) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "deadlineDays cannot be negative");
        }

        if (CollectionUtils.isEmpty(checklist.getTasks())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tasks is required for checklist " + checklist.getName());
        }

        for (TaskTemplateCreateItem task : checklist.getTasks()) {
            validateTask(companyId, task);
        }
    }

    private void validateTask(String companyId, TaskTemplateCreateItem task) {
        if (task == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "task item is required");
        }

        if (!StringUtils.hasText(task.getTitle())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "task title is required");
        }

        if (task.getDueDaysOffset() != null && task.getDueDaysOffset() < 0) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "dueDaysOffset cannot be negative for task " + task.getTitle());
        }

        boolean requireAck = Boolean.TRUE.equals(task.getRequireAck());
        boolean requireDoc = Boolean.TRUE.equals(task.getRequireDoc());

        List<String> requiredDocumentIds = normalizeRequiredDocumentIds(task.getRequiredDocumentIds());

        /*
         * Nghiệp vụ đúng hơn:
         * - requireDoc=true mới bắt buộc có requiredDocumentIds.
         * - requireAck=true chỉ là bắt nhân viên xác nhận, không nhất thiết phải có document.
         *
         * Nếu bạn vẫn muốn requireAck=true cũng bắt buộc document thì đổi điều kiện thành:
         * if ((requireAck || requireDoc) && CollectionUtils.isEmpty(requiredDocumentIds))
         */
        if (requireDoc && CollectionUtils.isEmpty(requiredDocumentIds)) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "requiredDocumentIds is required when requireDoc=true for task " + task.getTitle());
        }

        for (String documentId : requiredDocumentIds) {
            DocumentEntity document = documentMapper.selectByPrimaryKey(documentId);

            if (document == null || !companyId.equals(document.getCompanyId())) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid documentId: " + documentId);
            }

            if (StringUtils.hasText(document.getStatus())
                    && !"ACTIVE".equalsIgnoreCase(document.getStatus().trim())) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "document is not ACTIVE: " + documentId);
            }
        }
    }

    private static List<String> normalizeRequiredDocumentIds(List<String> requiredDocumentIds) {
        if (CollectionUtils.isEmpty(requiredDocumentIds)) {
            return List.of();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();

        for (String documentId : requiredDocumentIds) {
            if (StringUtils.hasText(documentId)) {
                normalized.add(documentId.trim());
            }
        }

        return List.copyOf(normalized);
    }
}