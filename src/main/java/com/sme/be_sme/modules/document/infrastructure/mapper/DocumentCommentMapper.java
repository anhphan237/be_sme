package com.sme.be_sme.modules.document.infrastructure.mapper;

import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentCommentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentCommentMapper {

    int insert(DocumentCommentEntity row);

    int updateByPrimaryKey(DocumentCommentEntity row);

    DocumentCommentEntity selectByPrimaryKey(@Param("documentCommentId") String documentCommentId);

    List<DocumentCommentEntity> selectByCompanyIdAndDocumentIdOrderByCreatedAsc(
            @Param("companyId") String companyId,
            @Param("documentId") String documentId,
            @Param("limit") int limit);
}
