package com.sme.be_sme.modules.platform.api.response;

import lombok.Data;

import java.util.List;

@Data
public class ListPlatformTemplateResponse {
    private List<PlatformTemplateListItemResponse> items;
    private Integer total;
    private Integer page;
    private Integer size;
}