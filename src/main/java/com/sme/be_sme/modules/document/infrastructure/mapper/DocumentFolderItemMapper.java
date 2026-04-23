package com.sme.be_sme.modules.document.infrastructure.mapper;

import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentFolderItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentFolderItemMapper {

    int insert(DocumentFolderItemEntity row);

    int deleteByCompanyIdAndDocumentId(@Param("companyId") String companyId, @Param("documentId") String documentId);

    DocumentFolderItemEntity selectByCompanyIdAndDocumentId(
            @Param("companyId") String companyId,
            @Param("documentId") String documentId);

    List<DocumentFolderItemEntity> selectByCompanyIdAndFolderId(
            @Param("companyId") String companyId,
            @Param("folderId") String folderId);
}
