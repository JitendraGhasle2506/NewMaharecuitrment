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

class V140MahaitEmployeeLeaveTourApprovalWorkflowTest {

    @Test
    void seedRuleIncludesRequiredAuditTimestamps() throws Exception {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        new V140__mahait_employee_leave_tour_approval_workflow().migrate(context);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(statement, atLeastOnce()).execute(sql.capture());
        List<String> statements = sql.getAllValues().stream()
                .map(value -> value.replaceAll("\\s+", " ").toLowerCase())
                .toList();

        assertThat(statements)
                .anySatisfy(value -> assertThat(value)
                        .contains("insert into approval_recruitment_type_rule")
                        .contains("recruitment_type_code, active, created_at, updated_at")
                        .contains("values ('mahait', true, current_timestamp, current_timestamp)"));
    }
}
