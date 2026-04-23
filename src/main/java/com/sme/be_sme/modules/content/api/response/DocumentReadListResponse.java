package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class DocumentReadListResponse {
    private String documentId;
    private List<ReadRow> items;

    @Getter
    @Setter
    public static class ReadRow {
        private String userId;
        private String status;
        private Date readAt;
        private Date ackedAt;
    }
}
