package com.sme.be_sme.modules.billing.infrastructure.mapper;

import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PaymentTransactionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface PaymentTransactionMapperExt {
    List<PaymentTransactionEntity> selectByCompanyId(@Param("companyId") String companyId);
    PaymentTransactionEntity selectByProviderTxnId(@Param("providerTxnId") String providerTxnId);
    int updateStatusByProviderTxnId(@Param("providerTxnId") String providerTxnId,
                                    @Param("status") String status,
                                    @Param("failureReason") String failureReason,
                                    @Param("paidAt") Date paidAt);
}
