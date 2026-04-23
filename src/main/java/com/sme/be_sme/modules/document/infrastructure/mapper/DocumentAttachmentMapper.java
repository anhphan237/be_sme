package com.sme.be_sme.modules.document.infrastructure.mapper;

import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentAttachmentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentAttachmentMapper {

    int insert(DocumentAttachmentEntity row);

    int deleteByPrimaryKey(@Param("documentAttachmentId") String documentAttachmentId);

    DocumentAttachmentEntity selectByPrimaryKey(@Param("documentAttachmentId") String documentAttachmentId);

    List<DocumentAttachmentEntity> selectActiveByCompanyAndDocument(
            @Param("companyId") String companyId,
            @Param("documentId") String documentId,
            @Param("limit") int limit);
}
