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
     * Update only plan_id, billing_cycle, status, updated_at. Avoids overwriting other columns.
     */
    int updatePlanAndStatus(@Param("subscriptionId") String subscriptionId,
                           @Param("planId") String planId,
                           @Param("billingCycle") String billingCycle,
                           @Param("status") String status,
                           @Param("updatedAt") Date updatedAt);

    int updateAutoRenewAndUpdatedAt(@Param("subscriptionId") String subscriptionId,
                                    @Param("autoRenew") Boolean autoRenew,
                                    @Param("updatedAt") Date updatedAt);
}
