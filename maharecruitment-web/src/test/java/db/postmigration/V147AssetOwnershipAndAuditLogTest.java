package db.postmigration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class V147AssetOwnershipAndAuditLogTest {

    @Test
    void migrationAddsOwnershipRentalDatesAndIpAuditLog() throws Exception {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        new V147__asset_ownership_and_audit_log().migrate(context);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(statement, atLeastOnce()).execute(sql.capture());
        List<String> statements = sql.getAllValues().stream()
                .map(value -> value.replaceAll("\\s+", " ").toLowerCase())
                .toList();

        assertThat(statements).anySatisfy(value -> assertThat(value)
                .contains("alter table asset_master")
                .contains("ownership_type")
                .contains("rent_start_date")
                .contains("rent_end_date"));
        assertThat(statements).anySatisfy(value -> assertThat(value)
                .contains("create table if not exists asset_audit_log")
                .contains("actor_username")
                .contains("ip_address")
                .contains("occurred_at"));
    }
}
