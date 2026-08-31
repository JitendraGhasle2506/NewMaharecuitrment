package db.postmigration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.Statement;

import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class V121EmployeeProfileMarriageDetailsTest {

    @Test
    void migrationAddsSpouseNameAndMarriageDate() throws Exception {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        new V121__employee_profile_marriage_details().migrate(context);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(statement).execute(sqlCaptor.capture());
        assertThat(sqlCaptor.getValue().replaceAll("\\s+", " ").trim().toLowerCase())
                .contains("alter table if exists employee_profile")
                .contains("add column if not exists spouse_name varchar(100)")
                .contains("add column if not exists marriage_date date");
    }
}
