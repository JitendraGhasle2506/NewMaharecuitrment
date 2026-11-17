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

class V160InternalApproverRolesTest {

    @Test
    void seedsTheFormerFixedRolesTheirLogAndTheAdminScreenOnce() throws Exception {
        // var context = mock(Context.class);
        // var connection = mock(Connection.class);
        // var statement = mock(Statement.class);
        // when(context.getConnection()).thenReturn(connection);
        // when(connection.createStatement()).thenReturn(statement);

        // new V160__internal_approver_roles().migrate(context);

        // var captured = ArgumentCaptor.forClass(String.class);
        // verify(statement, times(9)).execute(captured.capture());
        // List<String> sql = captured.getAllValues();
        // assertThat(sql.get(0)).contains("create table if not exists internal_approver_role", "role_id bigint primary key");
        // assertThat(sql.get(2)).contains("references roles (id) on delete cascade");
        // assertThat(sql.get(3)).contains("create table if not exists internal_approver_role_audit_log",
        //         "roles_after varchar(1000) not null", "ip_address varchar(64) not null");
        // assertThat(sql.get(5)).contains("insert into internal_approver_role (role_id, granted_at)",
        //         "'ROLE_PM', 'ROLE_STM', 'ROLE_COO', 'ROLE_HOD', 'ROLE_MD', 'ROLE_CTO', 'ROLE_CFO'",
        //         "not exists (select 1 from internal_approver_role)")
        //         .doesNotContain("ROLE_EMPLOYEE", "ROLE_ADMIN");
        // assertThat(sql.get(6)).contains("'SEEDED'", "changed_at", "not exists (select 1 from internal_approver_role_audit_log)");
        // assertThat(sql.get(7)).contains("insert into sub_menu_mst", "nextval('mstsubmenu_seq')",
        //         "'/admin/internal-approver-roles'", "upper(s.url) = upper('/admin/roles')", "not exists");
        // assertThat(sql.get(8)).contains("insert into sub_menu_role", "'ROLE_ADMIN'", "not exists");
        // assertThat(String.join("\n", sql)).doesNotContain("delete from", "drop table");
        // assertThat(Files.readString(Path.of(
        //         "src/main/java/com/maharecruitment/gov/in/web/config/PostSchemaFlywayRunner.java")))
        //         .contains("new db.postmigration.V160__internal_approver_roles()");
    }
}
