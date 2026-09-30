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

class V137EmailDispatchLogTest {

    @Test
    void migrationCreatesAuditTableAndSearchIndexes() throws Exception {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        new V137__email_dispatch_log().migrate(context);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(statement, times(4)).execute(sql.capture());
        String combinedSql = String.join(" ", sql.getAllValues()).replaceAll("\\s+", " ").toLowerCase();
        assertThat(combinedSql)
                .contains("create table if not exists email_dispatch_log")
                .contains("recipient_email varchar(320) not null")
                .contains("reason varchar(500) not null")
                .contains("status varchar(20) not null")
                .contains("idx_email_dispatch_recipient")
                .contains("idx_email_dispatch_status")
                .contains("idx_email_dispatch_requested");
    }
}
