package com.sme.be_sme.modules.document.infrastructure.mapper;

import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentLinkEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentLinkMapper {

    int insert(DocumentLinkEntity row);

    int deleteByPrimaryKey(@Param("documentLinkId") String documentLinkId);

    DocumentLinkEntity selectByPrimaryKey(@Param("documentLinkId") String documentLinkId);

    List<DocumentLinkEntity> selectActiveOutgoingByCompanyAndSource(
            @Param("companyId") String companyId,
            @Param("sourceDocumentId") String sourceDocumentId,
            @Param("limit") int limit);

    List<DocumentLinkEntity> selectActiveIncomingByCompanyAndTarget(
            @Param("companyId") String companyId,
            @Param("targetDocumentId") String targetDocumentId,
            @Param("limit") int limit);
}
