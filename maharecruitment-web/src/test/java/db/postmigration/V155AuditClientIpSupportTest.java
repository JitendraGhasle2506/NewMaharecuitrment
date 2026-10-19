package db.postmigration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;

import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class V155AuditClientIpSupportTest {
    @Test void addsIpToAllAuditTablesWithoutFabricatingHistoricalAddresses() throws Exception {
        var context = mock(Context.class);
        var connection = mock(Connection.class);
        var statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        new V155__audit_client_ip_support().migrate(context);
        var sql = ArgumentCaptor.forClass(String.class);
        verify(statement, times(10)).execute(sql.capture());
        assertThat(sql.getAllValues()).allSatisfy(command -> assertThat(command)
                .startsWith("alter table if exists ").contains("add column if not exists ip_address varchar(64)"));
        assertThat(String.join(" ", sql.getAllValues())).contains("audit_event", "team_management_audit_log",
                "employee_type_conversion_audit", "asset_audit_log", "login_logout_audit_history")
                .doesNotContain("update ");
        assertThat(Files.readString(Path.of("src/main/java/com/maharecruitment/gov/in/web/config/PostSchemaFlywayRunner.java")))
                .contains("new db.postmigration.V155__audit_client_ip_support()");
    }
}
