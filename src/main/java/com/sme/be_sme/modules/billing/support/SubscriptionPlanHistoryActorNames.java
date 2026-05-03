package com.sme.be_sme.modules.billing.support;

import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionPlanHistoryEntity;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapperExt;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves display names for {@code subscription_plan_history.changed_by} user ids.
 */
public final class SubscriptionPlanHistoryActorNames {

    private SubscriptionPlanHistoryActorNames() {
    }

    public static Map<String, String> loadDisplayNamesByChangedBy(
            String companyId,
            List<SubscriptionPlanHistoryEntity> rows,
            UserMapperExt userMapperExt) {
        Set<String> ids = new HashSet<>();
        if (rows != null) {
            for (SubscriptionPlanHistoryEntity row : rows) {
                if (row != null && StringUtils.hasText(row.getChangedBy())) {
                    ids.add(row.getChangedBy().trim());
                }
            }
        }
        Map<String, String> out = new HashMap<>();
        for (String userId : ids) {
            UserEntity u = userMapperExt.selectByCompanyIdAndUserId(companyId, userId);
            String name = u != null && StringUtils.hasText(u.getFullName()) ? u.getFullName().trim() : null;
            out.put(userId, name);
        }
        return out;
    }
}
