package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class DocumentVersionListResponse {
    private String documentId;
    private List<VersionItem> items;

    @Getter
    @Setter
    public static class VersionItem {
        private String documentVersionId;
        private Integer versionNo;
        private String fileUrl;
        private Boolean richTextSnapshot;
        private Date uploadedAt;
        private String uploadedBy;
    }
}
