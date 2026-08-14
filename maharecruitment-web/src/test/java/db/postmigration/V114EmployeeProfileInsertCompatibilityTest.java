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

class V114EmployeeProfileInsertCompatibilityTest {

    @Test
    void migrationMakesLegacyProfileRowsInsertable() throws Exception {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        new V114__employee_profile_insert_compatibility().migrate(context);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(statement, times(2)).execute(sqlCaptor.capture());
        List<String> statements = sqlCaptor.getAllValues().stream()
                .map(sql -> sql.replaceAll("\\s+", " ").trim().toLowerCase())
                .toList();

        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("alter column pan_no drop not null"));
        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("pg_get_serial_sequence('employee_profile', 'id')")
                .contains("alter column id set default nextval")
                .contains("setval(sequence_name::regclass"));
    }
}
