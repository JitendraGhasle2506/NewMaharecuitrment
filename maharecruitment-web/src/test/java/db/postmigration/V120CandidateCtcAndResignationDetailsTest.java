package db.postmigration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class V120CandidateCtcAndResignationDetailsTest {

    @Test
    void migrationAddsCtcResignationAndConditionalLastWorkingDayColumns() throws Exception {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        new V120__candidate_ctc_and_resignation_details().migrate(context);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(statement, times(7)).execute(sqlCaptor.capture());
        List<String> statements = sqlCaptor.getAllValues().stream()
                .map(sql -> sql.replaceAll("\\s+", " ").trim().toLowerCase())
                .toList();

        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("add column if not exists current_ctc numeric(14, 2)")
                .contains("add column if not exists has_resigned boolean")
                .contains("add column if not exists last_working_day date"));
        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("alter column has_resigned set default false")
                .contains("alter column has_resigned set not null"));
        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("recruitment_interview_last_working_day_check")
                .contains("has_resigned = true and last_working_day is not null")
                .contains("has_resigned = false and last_working_day is null"));
    }
}
