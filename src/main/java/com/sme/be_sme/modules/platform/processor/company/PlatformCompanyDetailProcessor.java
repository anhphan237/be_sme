package com.sme.be_sme.modules.platform.processor.company;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapper;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformCompanyDetailRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformCompanyDetailResponse;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PlatformCompanyDetailProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final CompanyMapper companyMapper;
    private final UserMapper userMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformCompanyDetailRequest request = objectMapper.convertValue(payload, PlatformCompanyDetailRequest.class);

        if (!StringUtils.hasText(request.getCompanyId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "companyId is required");
        }

        CompanyEntity company = companyMapper.selectByPrimaryKey(request.getCompanyId());
        if (company == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "Company not found");
        }

        int userCount = countUsersByCompany(request.getCompanyId());

        SubscriptionEntity subscription = findSubscriptionByCompany(request.getCompanyId());
        PlanEntity plan = null;
        if (subscription != null && subscription.getPlanId() != null) {
            plan = planMapper.selectByPrimaryKey(subscription.getPlanId());
        }

        PlatformCompanyDetailResponse response = new PlatformCompanyDetailResponse();
        response.setCompanyId(company.getCompanyId());
        response.setName(company.getName());
        response.setTaxCode(company.getTaxCode());
        response.setAddress(company.getAddress());
        response.setStatus(company.getStatus());
        response.setCreatedAt(company.getCreatedAt());
        response.setUserCount(userCount);

        if (subscription != null) {
            response.setSubscriptionId(subscription.getSubscriptionId());
            response.setSubscriptionStatus(subscription.getStatus());
            response.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd());
        }
        if (plan != null) {
            response.setPlanCode(plan.getCode());
            response.setPlanName(plan.getName());
        }

        return response;
    }

    private int countUsersByCompany(String companyId) {
        List<UserEntity> allUsers = userMapper.selectAll();
        int count = 0;
        for (UserEntity user : allUsers) {
            if (user != null && companyId.equals(user.getCompanyId())) {
                count++;
            }
        }
        return count;
    }

    private SubscriptionEntity findSubscriptionByCompany(String companyId) {
        List<SubscriptionEntity> all = subscriptionMapper.selectAll();
        for (SubscriptionEntity sub : all) {
            if (sub != null && companyId.equals(sub.getCompanyId())) {
                return sub;
            }
        }
        return null;
    }
}
