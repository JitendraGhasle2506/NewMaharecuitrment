package db.postmigration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.sql.Connection;
import java.sql.Statement;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class V154AdditionalChargeOptionalCellTest {
    @Test void allowsGlobalScopeWhilePreservingHistoricalDepartmentCharges() throws Exception {
        var context = mock(Context.class);
        var connection = mock(Connection.class);
        var statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        new V154__additional_charge_optional_cell().migrate(context);
        var sql = ArgumentCaptor.forClass(String.class);
        verify(statement).execute(sql.capture());
        assertThat(sql.getValue()).contains("drop constraint if exists chk_additional_charge_scope",
                "check (department_id is null or cell_id is null)").doesNotContain("update", "delete");
        assertThat(java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/maharecruitment/gov/in/web/config/PostSchemaFlywayRunner.java")))
                .contains("new db.postmigration.V154__additional_charge_optional_cell()");
    }
}
