package com.sme.be_sme.modules.billing.infrastructure.mapper;

import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.InvoiceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

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
     * Latest invoice still in ISSUED (awaiting payment) for the subscription, by issued_at desc.
     */
    InvoiceEntity selectLatestIssuedBySubscriptionId(
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

    /**
     * Invoices with due_at between fromDate and toDate, status ISSUED (unpaid).
     */
    List<InvoiceEntity> selectDueBetweenAndStatusIssued(
            @Param("fromDate") Date fromDate,
            @Param("toDate") Date toDate
    );

    int updateStatusIfCurrentIn(@Param("invoiceId") String invoiceId,
                                @Param("newStatus") String newStatus,
                                @Param("allowedStatus1") String allowedStatus1,
                                @Param("allowedStatus2") String allowedStatus2);

    List<InvoiceEntity> selectIssuedExpiredBefore(@Param("now") Date now);

    int markExpiredByInvoiceId(@Param("invoiceId") String invoiceId,
                               @Param("expiredAt") Date expiredAt);
}
