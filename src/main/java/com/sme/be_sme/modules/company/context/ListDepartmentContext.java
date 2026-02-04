package com.sme.be_sme.modules.company.context;

import com.sme.be_sme.modules.company.api.model.DepartmentItem;
import com.sme.be_sme.modules.company.api.request.ListDepartmentRequest;
import com.sme.be_sme.modules.company.api.response.ListDepartmentResponse;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.Data;

import java.util.List;

@Data
public class ListDepartmentContext {
    private BizContext biz;
    private ListDepartmentRequest request;
    private ListDepartmentResponse response;

    private List<DepartmentItem> items;
}
