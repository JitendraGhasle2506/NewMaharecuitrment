package com.maharecruitment.gov.in.recruitment.dto.organization;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionCsvImportResponse {

    private int totalRows;
    private int createdCount;
    private int failedCount;

    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
