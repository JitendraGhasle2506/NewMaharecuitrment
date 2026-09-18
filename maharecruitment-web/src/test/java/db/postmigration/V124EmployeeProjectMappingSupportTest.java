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

class V124EmployeeProjectMappingSupportTest {

    @Test
    void migrationCreatesDirectEmployeeProjectMapping() throws Exception {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        new V124__employee_project_mapping_support().migrate(context);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(statement, times(3)).execute(sql.capture());
        List<String> statements = sql.getAllValues().stream()
                .map(value -> value.replaceAll("\\s+", " ").trim().toLowerCase())
                .toList();
        assertThat(statements.getFirst())
                .contains("create table if not exists employee_project_mapping")
                .contains("foreign key (employee_id) references employee_master(employee_id)")
                .contains("foreign key (project_id) references project_mst(project_id)")
                .contains("unique (employee_id)");
    }
}
