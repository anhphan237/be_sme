package com.sme.be_sme.modules.platform.infrastructure.mapper;

import com.sme.be_sme.modules.platform.infrastructure.dto.FeedbackViewRow;
import com.sme.be_sme.modules.platform.infrastructure.dto.PlatformFeedbackRow;
import com.sme.be_sme.modules.platform.infrastructure.persistence.entity.FeedbackEntity;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FeedbackMapper {

    int insert(FeedbackEntity row);

    FeedbackEntity selectByPrimaryKey(@Param("feedbackId") String feedbackId);

    FeedbackEntity selectById(@Param("feedbackId") String feedbackId);

    List<FeedbackEntity> selectAll();

    int updateByPrimaryKey(FeedbackEntity row);

    List<PlatformFeedbackRow> selectPlatformFeedbackPage(
            @Param("companyId") String companyId,
            @Param("userId") String userId,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    long countPlatformFeedback(
            @Param("companyId") String companyId,
            @Param("userId") String userId,
            @Param("status") String status,
            @Param("keyword") String keyword
    );

    List<FeedbackViewRow> selectMyFeedbackPage(
            @Param("companyId") String companyId,
            @Param("userId") String userId,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    long countMyFeedback(
            @Param("companyId") String companyId,
            @Param("userId") String userId,
            @Param("status") String status,
            @Param("keyword") String keyword
    );

    int resolveFeedback(
            @Param("feedbackId") String feedbackId,
            @Param("status") String status,
            @Param("resolvedAt") OffsetDateTime resolvedAt,
            @Param("resolvedBy") String resolvedBy,
            @Param("resolutionNote") String resolutionNote
    );
}