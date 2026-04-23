package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class DocumentAttachmentListResponse {
    private String documentId;
    private List<AttachmentRow> items;

    @Getter
    @Setter
    public static class AttachmentRow {
        private String documentAttachmentId;
        private String fileUrl;
        private String fileName;
        private String fileType;
        private Long fileSizeBytes;
        private String mediaKind;
        private Date uploadedAt;
        private String uploadedBy;
    }
}
