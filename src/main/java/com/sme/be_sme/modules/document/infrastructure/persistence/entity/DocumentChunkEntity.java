package com.sme.be_sme.modules.document.infrastructure.persistence.entity;

import java.util.Date;

public class DocumentChunkEntity {
    private String chunkId;
    private String companyId;
    private String documentId;
    private Integer versionNo;
    private Integer chunkNo;
    private String chunkText;
    private Date createdAt;

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId == null ? null : chunkId.trim();
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId == null ? null : companyId.trim();
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId == null ? null : documentId.trim();
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public Integer getChunkNo() {
        return chunkNo;
    }

    public void setChunkNo(Integer chunkNo) {
        this.chunkNo = chunkNo;
    }

    public String getChunkText() {
        return chunkText;
    }

    public void setChunkText(String chunkText) {
        this.chunkText = chunkText;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
