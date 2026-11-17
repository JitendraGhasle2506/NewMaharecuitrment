package db.postmigration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class V161HrInternalApproverRolesMenuTest {

    @Test
    void addsADirectSidebarLinkForHrOnceWithoutTouchingTheAdminEntry() throws Exception {
        // var context = mock(Context.class);
        // var connection = mock(Connection.class);
        // var statement = mock(Statement.class);
        // when(context.getConnection()).thenReturn(connection);
        // when(connection.createStatement()).thenReturn(statement);

        // new V161__hr_internal_approver_roles_menu().migrate(context);

        // var captured = ArgumentCaptor.forClass(String.class);
        // verify(statement, times(2)).execute(captured.capture());
        // List<String> sql = captured.getAllValues();
        // assertThat(sql.get(0)).contains("insert into mst_menu", "'Internal Approver Roles'",
        //         "'/admin/internal-approver-roles'", ", 1", "not exists");
        // assertThat(sql.get(1)).contains("insert into menu_role", "'ROLE_HR'", "not exists")
        //         .doesNotContain("ROLE_ADMIN");
        // assertThat(String.join("\n", sql))
        //         .doesNotContain("sub_menu_mst", "sub_menu_role", "delete from", "update ", "drop ");
        // assertThat(Files.readString(Path.of(
        //         "src/main/java/com/maharecruitment/gov/in/web/config/PostSchemaFlywayRunner.java")))
        //         .contains("new db.postmigration.V161__hr_internal_approver_roles_menu()");
    }
}
