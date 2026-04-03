package com.sme.be_sme.modules.platform.service;

import com.sme.be_sme.modules.platform.infrastructure.mapper.ActivityLogMapper;
import com.sme.be_sme.modules.platform.infrastructure.persistence.entity.ActivityLogEntity;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogMapper activityLogMapper;

    public void log(String companyId, String userId, String action, String entityType, String entityId, String detail) {
        ActivityLogEntity entity = new ActivityLogEntity();
        entity.setLogId(UuidGenerator.generate());
        entity.setCompanyId(companyId);
        entity.setUserId(userId);
        entity.setAction(action);
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);
        entity.setDetail(detail);
        entity.setCreatedAt(new Date());
        activityLogMapper.insert(entity);
    }
}
