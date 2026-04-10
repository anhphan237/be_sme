package com.sme.be_sme.modules.billing.infrastructure.mapper;

import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionChangeRequestEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

@Mapper
public interface SubscriptionChangeRequestMapper {
    int insert(SubscriptionChangeRequestEntity row);

    SubscriptionChangeRequestEntity selectOpenBySubscriptionId(@Param("companyId") String companyId,
                                                               @Param("subscriptionId") String subscriptionId);

    SubscriptionChangeRequestEntity selectPendingByInvoiceId(@Param("companyId") String companyId,
                                                             @Param("invoiceId") String invoiceId);

    int markApplied(@Param("requestId") String requestId,
                    @Param("appliedAt") Date appliedAt,
                    @Param("updatedAt") Date updatedAt);

    int markFailed(@Param("requestId") String requestId,
                   @Param("failureReason") String failureReason,
                   @Param("updatedAt") Date updatedAt);
}
