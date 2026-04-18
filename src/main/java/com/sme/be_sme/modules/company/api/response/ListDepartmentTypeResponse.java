package com.sme.be_sme.modules.company.api.response;

import com.sme.be_sme.modules.company.api.model.DepartmentTypeItem;
import lombok.Data;

import java.util.List;

@Data
public class ListDepartmentTypeResponse {
    private List<DepartmentTypeItem> items;
}
