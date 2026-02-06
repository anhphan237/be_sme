package com.sme.be_sme.modules.billing.infrastructure.mapper;

import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.InvoiceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InvoiceMapperExt {

    /**
     * Latest unpaid invoice for the subscription (status != 'PAID'), by due_at desc.
     */
    InvoiceEntity selectLatestUnpaidBySubscriptionId(
            @Param("companyId") String companyId,
            @Param("subscriptionId") String subscriptionId
    );
}
