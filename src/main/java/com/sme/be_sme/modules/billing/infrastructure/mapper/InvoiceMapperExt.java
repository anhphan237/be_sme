package com.sme.be_sme.modules.billing.infrastructure.mapper;

import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.InvoiceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

@Mapper
public interface InvoiceMapperExt {

    /**
     * Latest unpaid invoice for the subscription (status != 'PAID'), by due_at desc.
     */
    InvoiceEntity selectLatestUnpaidBySubscriptionId(
            @Param("companyId") String companyId,
            @Param("subscriptionId") String subscriptionId
    );

    /**
     * Invoice for the subscription issued between the given dates (inclusive). Used by recurring job to avoid duplicates.
     */
    InvoiceEntity selectBySubscriptionIdAndIssuedBetween(
            @Param("companyId") String companyId,
            @Param("subscriptionId") String subscriptionId,
            @Param("issuedFrom") Date issuedFrom,
            @Param("issuedTo") Date issuedTo
    );
}
