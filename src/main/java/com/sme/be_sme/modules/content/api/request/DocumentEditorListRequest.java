package com.sme.be_sme.modules.content.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentEditorListRequest {
    private Integer page;
    private Integer pageSize;
    private String titleQuery;
}
