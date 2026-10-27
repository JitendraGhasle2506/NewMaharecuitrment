package db.postmigration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.sql.Connection;
import java.sql.Statement;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class V158AttendanceRequestAuditLogTest {
    @Test void createsDedicatedLeaveTourAuditTableWithIpAndLookupIndexes() throws Exception {
        var context = mock(Context.class);
        var connection = mock(Connection.class);
        var statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        new V158__attendance_request_audit_log().migrate(context);
        var sql = ArgumentCaptor.forClass(String.class);
        verify(statement, atLeastOnce()).execute(sql.capture());
        assertThat(String.join(" ", sql.getAllValues())).contains(
                "create table if not exists attendance_request_audit_log",
                "ip_address varchar(64) not null", "user_agent", "channel", "actor_designation", "actor_roles",
                "previous_status", "new_status", "previous_stage", "new_stage",
                "request_type in ('LEAVE', 'TOUR')",
                "(request_type, request_id, occurred_at desc)", "(employee_id, occurred_at desc)",
                "(actor_user_id, occurred_at desc)")
                .doesNotContain("drop ", "delete ");
        assertThat(java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/maharecruitment/gov/in/web/config/PostSchemaFlywayRunner.java")))
                .contains("new db.postmigration.V158__attendance_request_audit_log()");
    }
}
