package db.postmigration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.sql.Connection;
import java.sql.Statement;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class V152AdditionalChargeReplacementTest {
    @Test void addsReplacementIdentityAndSeparateTeamWithoutChangingPrimaryMappings() throws Exception {
        var context = mock(Context.class);
        var connection = mock(Connection.class);
        var statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        new V152__additional_charge_replacement_team().migrate(context);
        var sql = ArgumentCaptor.forClass(String.class);
        verify(statement, atLeastOnce()).execute(sql.capture());
        assertThat(String.join(" ", sql.getAllValues()))
                .contains("replaced_employee_id", "replaced_user_id", "additional_charge_team",
                        "fk_additional_charge_team_employee", "uk_additional_charge_team_member")
                .doesNotContain("update employee_reporting_mapping", "delete from employee_reporting_mapping");
        assertThat(java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/maharecruitment/gov/in/web/config/PostSchemaFlywayRunner.java")))
                .contains("new db.postmigration.V152__additional_charge_replacement_team()");
    }
}
