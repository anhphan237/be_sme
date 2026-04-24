package com.sme.be_sme.modules.document.infrastructure.mapper;

import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentFolderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentFolderMapper {

    int insert(DocumentFolderEntity row);

    int updateByPrimaryKey(DocumentFolderEntity row);

    DocumentFolderEntity selectByPrimaryKey(@Param("folderId") String folderId);

    List<DocumentFolderEntity> selectByCompanyId(@Param("companyId") String companyId);

    int countActiveChildrenByCompanyAndParentId(
            @Param("companyId") String companyId,
            @Param("parentFolderId") String parentFolderId);
}
