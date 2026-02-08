package com.sme.be_sme.modules.billing.infrastructure.mapper;

import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.DunningCaseEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface DunningCaseMapperExt {

    /**
     * One dunning case for the subscription (most recent by created_at).
     */
    DunningCaseEntity selectByCompanyIdAndSubscriptionId(
            @Param("companyId") String companyId,
            @Param("subscriptionId") String subscriptionId
    );

    /**
     * Cases due for retry: next_retry_at &lt;= asOf, status = PENDING_RETRY.
     */
    List<DunningCaseEntity> selectPendingRetryDue(@Param("asOf") Date asOf);
}
