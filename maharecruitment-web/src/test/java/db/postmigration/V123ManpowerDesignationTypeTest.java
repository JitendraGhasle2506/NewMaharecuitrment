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

class V123ManpowerDesignationTypeTest {

    @Test
    void migrationAddsBackfillsAndConstrainsDesignationType() throws Exception {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        new V123__manpower_designation_type().migrate(context);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(statement, times(5)).execute(sqlCaptor.capture());
        List<String> statements = sqlCaptor.getAllValues().stream()
                .map(sql -> sql.replaceAll("\\s+", " ").trim().toLowerCase())
                .toList();
        assertThat(statements)
                .anySatisfy(sql -> assertThat(sql)
                        .contains("add column if not exists designation_type varchar(1)"))
                .anySatisfy(sql -> assertThat(sql)
                        .contains("set designation_type = case")
                        .contains("else 'o'"))
                .anySatisfy(sql -> assertThat(sql)
                        .contains("alter column designation_type set default 'o'")
                        .contains("alter column designation_type set not null"))
                .anySatisfy(sql -> assertThat(sql)
                        .contains("check (designation_type in ('o', 'm'))"));
    }
}
