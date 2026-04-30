package com.sme.be_sme.modules.analytics.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.analytics.api.request.CandidateFitAssessRequest;
import com.sme.be_sme.modules.analytics.api.response.CandidateFitAssessResponse;
import com.sme.be_sme.modules.analytics.infrastructure.mapper.CandidateFitAuditMapper;
import com.sme.be_sme.modules.analytics.infrastructure.persistence.entity.CandidateFitAuditEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class CandidateFitAssessProcessor extends BaseBizProcessor<BizContext> {

    private static final double DEFAULT_JD_WEIGHT = 0.40d;
    private static final double DEFAULT_COMPETENCY_WEIGHT = 0.35d;
    private static final double DEFAULT_INTERVIEW_WEIGHT = 0.25d;
    private static final String FIT_TYPE_HIRING_CANDIDATE = "HIRING_CANDIDATE";

    private final ObjectMapper objectMapper;
    private final CandidateFitAuditMapper candidateFitAuditMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        CandidateFitAssessRequest request = objectMapper.convertValue(payload, CandidateFitAssessRequest.class);
        validate(context, request);

        double jdScore = normalizeScore(request.getJdMatchScore(), "jdMatchScore");
        double competencyScore = normalizeScore(request.getCompetencyScore(), "competencyScore");
        double interviewScore = normalizeScore(request.getInterviewScore(), "interviewScore");

        double jdWeight = request.getJdWeight() == null ? DEFAULT_JD_WEIGHT : request.getJdWeight();
        double competencyWeight = request.getCompetencyWeight() == null ? DEFAULT_COMPETENCY_WEIGHT : request.getCompetencyWeight();
        double interviewWeight = request.getInterviewWeight() == null ? DEFAULT_INTERVIEW_WEIGHT : request.getInterviewWeight();
        validateWeights(jdWeight, competencyWeight, interviewWeight);

        double fitScore = round2(jdScore * jdWeight + competencyScore * competencyWeight + interviewScore * interviewWeight);
        String fitLevel = resolveFitLevel(fitScore);
        Date now = new Date();

        CandidateFitAuditEntity audit = new CandidateFitAuditEntity();
        audit.setCandidateFitAuditId(UuidGenerator.generate());
        audit.setCompanyId(context.getTenantId().trim());
        audit.setEmployeeId(request.getEmployeeId().trim());
        audit.setJdMatchScore(jdScore);
        audit.setCompetencyScore(competencyScore);
        audit.setInterviewScore(interviewScore);
        audit.setJdWeight(jdWeight);
        audit.setCompetencyWeight(competencyWeight);
        audit.setInterviewWeight(interviewWeight);
        audit.setFitScore(fitScore);
        audit.setFitLevel(fitLevel);
        audit.setNote(StringUtils.hasText(request.getNote()) ? request.getNote().trim() : null);
        audit.setAssessedBy(StringUtils.hasText(context.getOperatorId()) ? context.getOperatorId().trim() : null);
        audit.setAssessedAt(now);
        audit.setCreatedAt(now);
        int inserted = candidateFitAuditMapper.insert(audit);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "insert candidate fit audit failed");
        }

        CandidateFitAssessResponse response = new CandidateFitAssessResponse();
        response.setAuditId(audit.getCandidateFitAuditId());
        response.setEmployeeId(audit.getEmployeeId());
        response.setFitType(FIT_TYPE_HIRING_CANDIDATE);
        response.setFitLevel(fitLevel);
        response.setFitScore(fitScore);
        CandidateFitAssessResponse.WeightConfig weightConfig = new CandidateFitAssessResponse.WeightConfig();
        weightConfig.setJdWeight(jdWeight);
        weightConfig.setCompetencyWeight(competencyWeight);
        weightConfig.setInterviewWeight(interviewWeight);
        response.setWeights(weightConfig);
        response.setAssessedAt(now);
        return response;
    }

    private static void validate(BizContext context, CandidateFitAssessRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getEmployeeId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "employeeId is required");
        }
        if (request.getJdMatchScore() == null || request.getCompetencyScore() == null || request.getInterviewScore() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "jdMatchScore, competencyScore, interviewScore are required");
        }
    }

    private static double normalizeScore(Double score, String fieldName) {
        if (score == null || score < 0d || score > 100d) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, fieldName + " must be in range [0,100]");
        }
        return score;
    }

    private static void validateWeights(double jdWeight, double competencyWeight, double interviewWeight) {
        if (jdWeight < 0d || competencyWeight < 0d || interviewWeight < 0d) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "weights must be >= 0");
        }
        double sum = jdWeight + competencyWeight + interviewWeight;
        if (Math.abs(sum - 1.0d) > 0.000001d) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "weights must sum to 1.0");
        }
    }

    private static String resolveFitLevel(double fitScore) {
        if (fitScore >= 80d) {
            return "HIGH";
        }
        if (fitScore >= 60d) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private static double round2(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }
}

