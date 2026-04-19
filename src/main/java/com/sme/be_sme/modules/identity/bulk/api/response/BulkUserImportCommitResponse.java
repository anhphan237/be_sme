package com.sme.be_sme.modules.identity.bulk.api.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BulkUserImportCommitResponse {
    private int totalRows;
    private int createdRows;
    private int failedRows;
    private List<RowResult> rows = new ArrayList<>();

    @Getter
    @Setter
    public static class RowResult {
        private int rowNumber;
        private String email;
        private String fullName;
        private String status;
        private String userId;
        private List<String> errors = new ArrayList<>();
    }
}
