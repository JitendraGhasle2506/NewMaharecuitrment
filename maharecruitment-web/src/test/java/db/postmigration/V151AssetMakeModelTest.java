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

class V151AssetMakeModelTest {

    @Test
    void addsMakeModelToAssetMaster() throws Exception {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        new V151__asset_make_model().migrate(context);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(statement).execute(sql.capture());
        assertThat(sql.getValue().replaceAll("\\s+", " ").toLowerCase())
                .contains("alter table asset_master")
                .contains("make_model varchar(150)");
    }
}
