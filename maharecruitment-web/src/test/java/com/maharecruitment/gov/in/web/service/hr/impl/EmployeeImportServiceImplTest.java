package com.maharecruitment.gov.in.web.service.hr.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class EmployeeImportServiceImplTest {

    @Test
    void downloadedCsvTemplateContainsAlignedMahaitSample() {
        EmployeeImportServiceImpl service = new EmployeeImportServiceImpl(
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);

        String[] rows = new String(service.buildCsvTemplate(), StandardCharsets.UTF_8).split("\\R");

        assertThat(rows).hasSize(4);
        assertThat(rows[3]).contains(",MAHAIT,Sneha Deshmukh,");
        assertThat(rows)
                .allSatisfy(row -> assertThat(row.split(",", -1)).hasSize(27));
    }
}
