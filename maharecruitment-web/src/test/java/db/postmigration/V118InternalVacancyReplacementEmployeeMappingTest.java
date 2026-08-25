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

class V118InternalVacancyReplacementEmployeeMappingTest {

    @Test
    void migrationAddsIndexedEmployeeForeignKey() throws Exception {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        new V118__internal_vacancy_replacement_employee_mapping().migrate(context);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(statement, times(3)).execute(sqlCaptor.capture());
        List<String> statements = sqlCaptor.getAllValues().stream()
                .map(sql -> sql.replaceAll("\\s+", " ").trim().toLowerCase())
                .toList();

        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("add column if not exists replacement_employee_id bigint"));
        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("create index if not exists idx_internal_vacancy_replacement_employee"));
        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("constraint fk_internal_vacancy_replacement_employee")
                .contains("references employee_master(employee_id)"));
    }
}
