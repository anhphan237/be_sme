package com.sme.be_sme.modules.document.infrastructure.mapper;

import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentChunkEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentChunkMapper {

    int insertBatch(@Param("chunks") List<DocumentChunkEntity> chunks);

    int deleteByDocumentIdAndVersion(@Param("companyId") String companyId,
                                    @Param("documentId") String documentId,
                                    @Param("versionNo") int versionNo);

    List<DocumentChunkEntity> selectByCompanyId(@Param("companyId") String companyId);
}
