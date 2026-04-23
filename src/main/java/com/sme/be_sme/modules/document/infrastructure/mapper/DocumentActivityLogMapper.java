package com.sme.be_sme.modules.document.infrastructure.mapper;

import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentActivityLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentActivityLogMapper {

    int insert(DocumentActivityLogEntity row);

    List<DocumentActivityLogEntity> selectRecentByDocumentId(
            @Param("companyId") String companyId,
            @Param("documentId") String documentId,
            @Param("limit") int limit);
}
