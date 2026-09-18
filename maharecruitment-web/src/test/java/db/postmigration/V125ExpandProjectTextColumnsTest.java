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

class V125ExpandProjectTextColumnsTest {

    @Test
    void migrationExpandsProjectNameAndDescriptionColumns() throws Exception {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        new V125__expand_project_text_columns().migrate(context);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(statement, times(2)).execute(sql.capture());
        List<String> statements = sql.getAllValues().stream()
                .map(value -> value.replaceAll("\\s+", " ").trim().toLowerCase())
                .toList();
        assertThat(statements)
                .anySatisfy(value -> assertThat(value)
                        .contains("alter column project_name type varchar(300)"))
                .anySatisfy(value -> assertThat(value)
                        .contains("alter column project_desc type varchar(500)"));
    }
}
