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

class V102EmployeeLocationMappingPrimarySupportTest {

    @Test
    void migrationBackfillsOldestMappingAndPreventsMultiplePrimaries() throws Exception {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        new V102__employee_location_mapping_primary_support().migrate(context);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(statement, times(3)).execute(sqlCaptor.capture());
        List<String> statements = sqlCaptor.getAllValues().stream()
                .map(sql -> sql.replaceAll("\\s+", " ").trim().toLowerCase())
                .toList();

        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("add column if not exists is_primary boolean not null default false"));
        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("row_number() over")
                .contains("partition by employee_id")
                .contains("order by created_date_time asc nulls last, employee_location_mapping_id asc")
                .contains("set is_primary = (ranked.mapping_rank = 1)"));
        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("create unique index if not exists uk_employee_location_mapping_one_primary")
                .contains("on employee_location_mapping (employee_id)")
                .contains("where is_primary = true"));
    }
}
