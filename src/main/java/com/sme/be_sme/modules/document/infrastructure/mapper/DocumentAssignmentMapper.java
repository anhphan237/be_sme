package com.sme.be_sme.modules.document.infrastructure.mapper;

import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentAssignmentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentAssignmentMapper {

    int insert(DocumentAssignmentEntity row);

    int updateByPrimaryKey(DocumentAssignmentEntity row);

    DocumentAssignmentEntity selectByPrimaryKey(@Param("documentAssignmentId") String documentAssignmentId);

    List<DocumentAssignmentEntity> selectActiveByCompanyAndDocument(
            @Param("companyId") String companyId,
            @Param("documentId") String documentId,
            @Param("limit") int limit);

    DocumentAssignmentEntity selectLatestByCompanyDocumentAndAssignee(
            @Param("companyId") String companyId,
            @Param("documentId") String documentId,
            @Param("assigneeUserId") String assigneeUserId);

    DocumentAssignmentEntity selectActiveByCompanyDocumentAndAssignee(
            @Param("companyId") String companyId,
            @Param("documentId") String documentId,
            @Param("assigneeUserId") String assigneeUserId);
}
