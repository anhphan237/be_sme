package com.sme.be_sme.modules.billing.infrastructure.mapper;

import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PaymentProviderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentProviderMapper {
    int insert(PaymentProviderEntity entity);
    PaymentProviderEntity selectByPrimaryKey(String paymentProviderId);
    List<PaymentProviderEntity> selectByCompanyId(@Param("companyId") String companyId);
    PaymentProviderEntity selectByCompanyIdAndProviderName(@Param("companyId") String companyId,
                                                           @Param("providerName") String providerName);
    int updateByPrimaryKey(PaymentProviderEntity entity);
}
