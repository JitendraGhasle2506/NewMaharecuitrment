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

class V119InternalVacancyMultipleReplacementEmployeesTest {

    @Test
    void migrationCreatesJoinTableMigratesExistingEmployeeAndRemovesLegacyColumns() throws Exception {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        new V119__internal_vacancy_multiple_replacement_employees().migrate(context);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(statement, times(7)).execute(sqlCaptor.capture());
        List<String> statements = sqlCaptor.getAllValues().stream()
                .map(sql -> sql.replaceAll("\\s+", " ").trim().toLowerCase())
                .toList();

        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("create table if not exists internal_vacancy_replacement_employee")
                .contains("unique (internal_vacancy_opening_id, employee_id)")
                .contains("references employee_master(employee_id)"));
        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("insert into internal_vacancy_replacement_employee")
                .contains("select internal_vacancy_opening_id, replacement_employee_id"));
        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("drop column if exists replacement_employee_id")
                .contains("drop column if exists replacement_employee_name"));
    }
}
