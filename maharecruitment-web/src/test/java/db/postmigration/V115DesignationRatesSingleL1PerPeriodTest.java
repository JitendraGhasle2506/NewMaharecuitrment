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

class V115DesignationRatesSingleL1PerPeriodTest {

    @Test
    void migrationKeepsOneRatePerDesignationPeriodAndNormalizesItToL1() throws Exception {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        new V115__designation_rates_single_l1_per_period().migrate(context);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(statement, times(2)).execute(sqlCaptor.capture());
        List<String> statements = sqlCaptor.getAllValues().stream()
                .map(sql -> sql.replaceAll("\\s+", " ").trim().toLowerCase())
                .toList();

        assertThat(statements.get(0))
                .contains("partition by designation_id, effective_from")
                .contains("upper(trim(active_flag)) = 'y'")
                .contains("upper(trim(level_code)) = 'l1'")
                .contains("delete from manpower_designation_rate")
                .contains("ranked.row_rank > 1");
        assertThat(statements.get(1))
                .contains("update manpower_designation_rate")
                .contains("set level_code = 'l1'")
                .contains("where level_code is distinct from 'l1'");
    }
}
