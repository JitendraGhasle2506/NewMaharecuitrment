package db.postmigration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.sql.Connection;
import java.sql.Statement;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class V159ManageResourcesMenuTest {
    @Test void seedsDbMenuForLeadershipRolesIdempotently() throws Exception {
        var context = mock(Context.class);
        var connection = mock(Connection.class);
        var statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        new V159__manage_resources_menu().migrate(context);
        var sql = ArgumentCaptor.forClass(String.class);
        verify(statement, times(2)).execute(sql.capture());
        assertThat(sql.getAllValues().get(0)).contains("insert into mst_menu", "'/reporting-manager/resources'",
                "'Manage Resources'", "where not exists");
        assertThat(sql.getAllValues().get(1)).contains("insert into menu_role",
                "'ROLE_HOD', 'ROLE_COO', 'ROLE_CTO', 'ROLE_CFO'", "where upper(m.url)", "not exists")
                .doesNotContain("ROLE_EMPLOYEE", "ROLE_MD", "ROLE_STM", "ROLE_PM", "delete");
        assertThat(java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/maharecruitment/gov/in/web/config/PostSchemaFlywayRunner.java")))
                .contains("new db.postmigration.V159__manage_resources_menu()");
        assertThat(java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/resources/templates/header/header.html")))
                .doesNotContain("/reporting-manager/resources");
    }
}
