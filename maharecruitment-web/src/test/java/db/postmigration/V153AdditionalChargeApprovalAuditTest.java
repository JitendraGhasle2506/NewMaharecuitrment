package db.postmigration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.sql.Connection;
import java.sql.Statement;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class V153AdditionalChargeApprovalAuditTest {
    @Test void retainsOriginalRoutesAndAddsActualDecisionActorsAndHistoryIndexes() throws Exception {
        var context = mock(Context.class);
        var connection = mock(Connection.class);
        var statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        new V153__additional_charge_approval_audit().migrate(context);
        var sql = ArgumentCaptor.forClass(String.class);
        verify(statement, atLeastOnce()).execute(sql.capture());
        assertThat(String.join(" ", sql.getAllValues())).contains("alter table leave_application", "alter table tour_application",
                "manager_decision_by_user_id", "hod_decision_by_user_id", "where hod_decision_by_user_id is not null")
                .doesNotContain("update leave_application", "update tour_application");
        assertThat(java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/maharecruitment/gov/in/web/config/PostSchemaFlywayRunner.java")))
                .contains("new db.postmigration.V153__additional_charge_approval_audit()");
    }
}
