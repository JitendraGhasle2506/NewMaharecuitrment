package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class PreOnboardingSchemaGuardRunnerTest {
    @Test
    void completedSchemaSkipsAllRedundantAlterationsAndBackfills() throws Exception {
        Connection connection = mock(Connection.class);
        var metadata = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
        var preColumns = mock(PreparedStatement.class);
        var employeeColumns = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(preColumns, employeeColumns);
        ResultSet preRows = columns(List.of("hr_onboarding_date", "hr_onboarding_location",
                "hr_verified", "hr_user_id", "onboarded_at", "gender", "blood_group", "emergency_contact_name",
                "emergency_contact_relation", "emergency_contact_mobile", "emergency_contact_alt_mobile",
                "company_payroll_more_than_three_months", "company_payroll_proof_original_name",
                "company_payroll_proof_file_path", "company_payroll_proof_file_type", "company_payroll_proof_file_size",
                "submitted_at", "onboarding_date"));
        ResultSet employeeRows = columns(List.of("gender", "blood_group",
                "emergency_contact_name", "emergency_contact_relation", "emergency_contact_mobile",
                "emergency_contact_alt_mobile", "company_payroll_more_than_three_months"));
        when(preColumns.executeQuery()).thenReturn(preRows);
        when(employeeColumns.executeQuery()).thenReturn(employeeRows);
        Statement changes = mock(Statement.class);
        when(connection.createStatement()).thenReturn(changes);
        assertThat(PreOnboardingSchemaGuardRunner.apply(connection)).isFalse();
        verify(changes, never()).execute(anyString());
        verify(metadata, never()).getColumns(any(), any(), any(), any());
    }

    private ResultSet columns(List<String> names) throws Exception {
        ResultSet rows = mock(ResultSet.class);
        AtomicInteger index = new AtomicInteger(-1);
        when(rows.next()).thenAnswer(call -> index.incrementAndGet() < names.size());
        when(rows.getString(1)).thenAnswer(call -> names.get(index.get()));
        when(rows.getBoolean(2)).thenAnswer(call -> names.get(index.get()).equals("submitted_at"));
        return rows;
    }
}
