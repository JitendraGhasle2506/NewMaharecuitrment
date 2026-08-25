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

class V117InternalVacancyHiringRequestDetailsTest {

    @Test
    void migrationAddsHiringTypeReplacementAndApprovalDocumentColumns() throws Exception {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        new V117__internal_vacancy_hiring_request_details().migrate(context);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(statement, times(11)).execute(sqlCaptor.capture());
        List<String> statements = sqlCaptor.getAllValues().stream()
                .map(sql -> sql.replaceAll("\\s+", " ").trim().toLowerCase())
                .toList();

        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("add column if not exists hiring_request_type varchar(30)")
                .doesNotContain("not null"));
        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("set hiring_request_type = 'new_candidate'")
                .contains("where hiring_request_type is null"));
        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("alter column hiring_request_type set not null"));
        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("add column if not exists replacement_employee_name varchar(200)"));
        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("add column if not exists e_office_approval_file_path varchar(1000)"));
        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("check (hiring_request_type in ('new_candidate', 'employee_replacement'))"));
    }
}
