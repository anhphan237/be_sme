package com.sme.be_sme.modules.content.doceditor;

import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentAccessRuleMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentAccessRuleEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapperExt;
import com.sme.be_sme.modules.employee.infrastructure.persistence.entity.EmployeeProfileEntity;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserRoleMapperExt;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fine-grained access for EDITOR documents via {@code document_access_rules}.
 * <p>Semantic A: if a document has no ACTIVE rules, access follows existing tenant + JWT permission checks only.
 * HR / HR_ADMIN / MANAGER / ADMIN always bypass rule checks for EDITOR docs.</p>
 */
@Component
@RequiredArgsConstructor
public class DocumentAccessEvaluator {

    private final DocumentAccessRuleMapper documentAccessRuleMapper;
    private final UserRoleMapperExt userRoleMapperExt;
    private final EmployeeProfileMapperExt employeeProfileMapperExt;

    public boolean isManagementBypass(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        Set<String> upper = roles.stream()
                .filter(StringUtils::hasText)
                .map(r -> r.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        return upper.contains("ADMIN")
                || upper.contains("HR")
                || upper.contains("HR_ADMIN")
                || upper.contains("MANAGER");
    }

    /**
     * Resolves role UUIDs and department id for listing documents with access-rule SQL (non-management users).
     */
    public EditorAccessSubject resolveSubject(BizContext ctx) {
        String companyId = ctx.getTenantId();
        String userId = ctx.getOperatorId();
        List<String> roleIds = userRoleMapperExt.selectRoleIdsByCompanyAndUser(companyId, userId);
        if (roleIds == null) {
            roleIds = Collections.emptyList();
        }
        EmployeeProfileEntity profile = employeeProfileMapperExt.selectByCompanyIdAndUserId(companyId, userId);
        String departmentId = profile != null ? profile.getDepartmentId() : null;
        return new EditorAccessSubject(roleIds, departmentId);
    }

    public void assertCanAccess(BizContext ctx, DocumentEntity doc) {
        assertNotSoftDeleted(doc);
        applyEditorRuleChecks(ctx, doc);
    }

    /**
     * Like {@link #assertCanAccess} but ignores {@link DocumentEditorConstants#STATUS_DELETED} so callers may
     * authorize idempotent soft-delete after the document row is already marked deleted.
     */
    public void assertCanMutateIncludingSoftDeleted(BizContext ctx, DocumentEntity doc) {
        if (doc == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        applyEditorRuleChecks(ctx, doc);
    }

    private static void assertNotSoftDeleted(DocumentEntity doc) {
        if (doc == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        if (DocumentEditorConstants.STATUS_DELETED.equalsIgnoreCase(
                doc.getStatus() != null ? doc.getStatus().trim() : "")) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
    }

    private void applyEditorRuleChecks(BizContext ctx, DocumentEntity doc) {
        if (!DocumentEditorConstants.CONTENT_KIND_EDITOR.equalsIgnoreCase(doc.getContentKind())) {
            return;
        }
        if (isManagementBypass(ctx.getRoles())) {
            return;
        }
        String companyId = ctx.getTenantId();
        List<DocumentAccessRuleEntity> rules =
                documentAccessRuleMapper.selectActiveByCompanyAndDocumentId(companyId, doc.getDocumentId());
        if (rules == null || rules.isEmpty()) {
            return;
        }
        EditorAccessSubject subject = resolveSubject(ctx);
        for (DocumentAccessRuleEntity rule : rules) {
            if (ruleMatches(rule, subject)) {
                return;
            }
        }
        throw AppException.of(ErrorCodes.FORBIDDEN, "document access denied");
    }

    private static boolean ruleMatches(DocumentAccessRuleEntity rule, EditorAccessSubject subject) {
        boolean roleOk = !StringUtils.hasText(rule.getRoleId())
                || subject.roleIds().contains(rule.getRoleId());
        boolean deptOk = !StringUtils.hasText(rule.getDepartmentId())
                || (StringUtils.hasText(subject.departmentId()) && subject.departmentId().equals(rule.getDepartmentId()));
        return roleOk && deptOk;
    }

    public record EditorAccessSubject(List<String> roleIds, String departmentId) {}
}
