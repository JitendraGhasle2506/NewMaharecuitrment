package com.maharecruitment.gov.in.web.dto.hr;

import java.util.List;

public record EmployeeImportResult(
        int totalRows,
        int successCount,
        int failureCount,
        List<EmployeeImportRowResult> rows) {

    public EmployeeImportResult {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    public boolean hasFailures() {
        return failureCount > 0;
    }
}
