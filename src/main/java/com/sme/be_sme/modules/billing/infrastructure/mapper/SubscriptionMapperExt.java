package com.sme.be_sme.modules.billing.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/**
 * Extended mapper for subscription operations.
 * Provides selective update to avoid full-entity update issues (e.g. PostgreSQL boolean/BIT).
 */
@Mapper
public interface SubscriptionMapperExt {

    /**
     * Update only plan_id, status, updated_at. Avoids overwriting other columns.
     */
    int updatePlanAndStatus(@Param("subscriptionId") String subscriptionId,
                           @Param("planId") String planId,
                           @Param("status") String status,
                           @Param("updatedAt") Date updatedAt);
}
