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

class V105CellReportingAuthorityMappingSupportTest {

    @Test
    void migrationCreatesUniqueCellAuthorityMappingAndLookupIndex() throws Exception {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        new V105__cell_reporting_authority_mapping_support().migrate(context);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(statement, times(3)).execute(sqlCaptor.capture());
        List<String> statements = sqlCaptor.getAllValues().stream()
                .map(sql -> sql.replaceAll("\\s+", " ").trim().toLowerCase())
                .toList();

        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("create table if not exists cell_reporting_authority_mapping")
                .contains("foreign key (cell_id) references m_cell_master(cell_id)")
                .contains("foreign key (authority_user_id) references users(id)")
                .contains("unique (cell_id)"));
        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("create index if not exists idx_cell_reporting_authority_user")
                .contains("on cell_reporting_authority_mapping (authority_user_id)"));
        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("create index if not exists idx_employee_reporting_mapping_employee")
                .contains("on employee_reporting_mapping (employee_id)"));
    }
}
