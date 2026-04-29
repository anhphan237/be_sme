package com.sme.be_sme.modules.document.infrastructure.mapper;

import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentBlockEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentBlockMapper {
    int insert(DocumentBlockEntity row);

    int updateByPrimaryKey(DocumentBlockEntity row);

    int deleteByPrimaryKey(@Param("documentBlockId") String documentBlockId);

    int softDeleteByPrimaryKey(@Param("documentBlockId") String documentBlockId,
                               @Param("updatedAt") java.util.Date updatedAt);

    int deleteByCompanyAndDocumentId(@Param("companyId") String companyId,
                                     @Param("documentId") String documentId);

    DocumentBlockEntity selectByPrimaryKey(@Param("documentBlockId") String documentBlockId);

    List<DocumentBlockEntity> selectActiveByCompanyAndDocumentId(@Param("companyId") String companyId,
                                                                  @Param("documentId") String documentId);
}
