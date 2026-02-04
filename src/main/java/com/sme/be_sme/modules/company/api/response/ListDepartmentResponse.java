package com.sme.be_sme.modules.company.api.response;

import com.sme.be_sme.modules.company.api.model.DepartmentItem;
import lombok.Data;

import java.util.List;

@Data
public class ListDepartmentResponse {
    private List<DepartmentItem> items;
}


