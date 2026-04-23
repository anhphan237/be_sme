package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentCreateDraftResponse {
    private String documentId;
    private String title;
    private String status;
}
