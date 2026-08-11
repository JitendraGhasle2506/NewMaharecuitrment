package db.postmigration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class V106ProjectMasterDepartmentMappingTest {

    @Test
    void migrationBackfillsCanonicalDepartmentMappingsAndRemovesRegistrationColumn() throws Exception {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        new V106__project_master_department_mapping().migrate(context);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(statement, times(11)).execute(sqlCaptor.capture());
        List<String> statements = sqlCaptor.getAllValues().stream()
                .map(sql -> sql.replaceAll("\\s+", " ").trim().toLowerCase())
                .toList();

        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("add column if not exists department_id bigint")
                .contains("add column if not exists sub_department_id bigint"));
        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("from department_project_application application")
                .contains("project.application_id = application.department_project_application_id")
                .contains("where department.department_id = application.department_id")
                .contains("where sub_department.sub_dept_id = application.sub_department_id")
                .contains("else null"));
        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("project.department_registration_id = registration.department_registration_id")
                .contains("where department.department_id = registration.department_id")
                .contains("where sub_department.sub_dept_id = registration.sub_department_id"));
        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("foreign key (department_id)")
                .contains("references department_mst(department_id)"));
        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("foreign key (sub_department_id)")
                .contains("references sub_department(sub_dept_id)"));
        assertThat(statements).anySatisfy(sql -> assertThat(sql)
                .contains("drop column if exists department_registration_id"));
    }
}
