package db.postmigration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.Statement;

import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class V116DesignationRates2026L1DefaultsTest {

    @Test
    void migrationInsertsOnlyMissing2026L1RatesForRequestedDesignations() throws Exception {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        new V116__designation_rates_2026_l1_defaults().migrate(context);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(statement, times(2)).execute(sqlCaptor.capture());
        String sequenceSql = sqlCaptor.getAllValues().get(0).replaceAll("\\s+", " ").trim().toLowerCase();
        String insertSql = sqlCaptor.getAllValues().get(1).replaceAll("\\s+", " ").trim().toLowerCase();

        assertThat(sequenceSql)
                .contains("pg_get_serial_sequence('manpower_designation_rate', 'rate_id')")
                .contains("coalesce(max(rate_id), 0) + 1");
        assertThat(insertSql)
                .contains("designation.designation_id between 1 and 89")
                .contains("'l1'")
                .contains("date '2026-01-01'")
                .contains("date '2026-12-31'")
                .contains("not exists")
                .contains("on conflict (designation_id, level_code, effective_from) do nothing");
    }
}
