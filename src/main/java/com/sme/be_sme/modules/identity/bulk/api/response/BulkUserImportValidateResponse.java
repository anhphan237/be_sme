package com.sme.be_sme.modules.identity.bulk.api.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BulkUserImportValidateResponse {
    private int totalRows;
    private int validRows;
    private int invalidRows;
    private List<RowResult> rows = new ArrayList<>();

    @Getter
    @Setter
    public static class RowResult {
        private int rowNumber;
        private String email;
        private String fullName;
        private String status;
        private List<String> errors = new ArrayList<>();
    }
}
