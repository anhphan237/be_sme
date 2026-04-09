package com.sme.be_sme.modules.billing.infrastructure.mapper;

import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionPlanHistoryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface SubscriptionPlanHistoryMapper {
    int insert(SubscriptionPlanHistoryEntity row);

    int closeOpenBySubscription(@Param("companyId") String companyId,
                                @Param("subscriptionId") String subscriptionId,
                                @Param("effectiveTo") Date effectiveTo);

    int countByCompanyAndPeriod(@Param("companyId") String companyId,
                                @Param("fromTs") Date fromTs,
                                @Param("toTs") Date toTs);

    List<SubscriptionPlanHistoryEntity> selectByCompanyAndPeriod(@Param("companyId") String companyId,
                                                                 @Param("fromTs") Date fromTs,
                                                                 @Param("toTs") Date toTs,
                                                                 @Param("limit") int limit,
                                                                 @Param("offset") int offset);
}
