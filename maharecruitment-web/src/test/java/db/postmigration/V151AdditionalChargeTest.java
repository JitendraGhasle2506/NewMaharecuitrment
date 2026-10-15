package db.postmigration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.sql.Connection;
import java.sql.Statement;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class V151AdditionalChargeTest {
    @Test void createsSeparateChargeHistoryWithIdentityScopeAndDateConstraints() throws Exception {
        var context = mock(Context.class);
        var connection = mock(Connection.class);
        var statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        new V151__employee_additional_charge().migrate(context);
        var sql = ArgumentCaptor.forClass(String.class);
        verify(statement, atLeastOnce()).execute(sql.capture());
        assertThat(String.join(" ", sql.getAllValues())).contains("chk_additional_charge_assignee",
                "chk_additional_charge_scope", "chk_additional_charge_dates", "revoked_by", "assigned_by");
        assertThat(java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/maharecruitment/gov/in/web/config/PostSchemaFlywayRunner.java")))
                .contains("new db.postmigration.V151__employee_additional_charge()");
    }
}
