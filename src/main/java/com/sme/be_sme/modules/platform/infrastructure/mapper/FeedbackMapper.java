package com.sme.be_sme.modules.platform.infrastructure.mapper;

import com.sme.be_sme.modules.platform.infrastructure.persistence.entity.FeedbackEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FeedbackMapper {
    int insert(FeedbackEntity row);

    FeedbackEntity selectByPrimaryKey(String feedbackId);

    List<FeedbackEntity> selectAll();

    int updateByPrimaryKey(FeedbackEntity row);
}
