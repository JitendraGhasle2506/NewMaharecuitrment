package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MigrationLockTimeoutDataSourceTest {
    private final DataSource source = mock(DataSource.class);
    private final Connection connection = mock(Connection.class);
    private final PreparedStatement statement = mock(PreparedStatement.class);
    private final AtomicBoolean autoCommit = new AtomicBoolean(true);

    @BeforeEach
    void setUp() throws Exception {
        when(source.getConnection()).thenReturn(connection);
        var metadata = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.getAutoCommit()).thenAnswer(call -> autoCommit.get());
        doAnswer(call -> { autoCommit.set(call.getArgument(0)); return null; }).when(connection).setAutoCommit(anyBoolean());
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        var settings = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(settings);
        when(settings.next()).thenReturn(true);
        when(settings.getString(1)).thenReturn("0");
    }

    @Test
    void restoresThePoolSettingAfterMigrationAndRollsBackAnUnfinishedTransaction() throws Exception {
        Connection migration = new MigrationLockTimeoutDataSource(source).getConnection();
        verify(connection).prepareStatement("select set_config('lock_timeout', '5s', false)");
        migration.setAutoCommit(false);
        migration.close();
        var order = inOrder(connection, statement);
        order.verify(connection).rollback();
        order.verify(connection).setAutoCommit(true);
        order.verify(connection).prepareStatement("select set_config('lock_timeout', ?, false)");
        order.verify(statement).setString(1, "0");
        order.verify(statement).execute();
        order.verify(connection).close();
        assertThat(autoCommit).isTrue();
    }

    @Test
    void propagatesOriginalSqlExceptionAndDiscardsConnectionWhenResetFails() throws Exception {
        Connection migration = new MigrationLockTimeoutDataSource(source).getConnection();
        when(statement.execute()).thenThrow(new SQLException("reset failed"));
        assertThatThrownBy(migration::close).isInstanceOf(SQLException.class).hasMessage("reset failed");
        verify(connection).abort(any());
        verify(connection).close();
    }

    @Test
    void closesConnectionWhenInitialSetupFails() throws Exception {
        when(statement.executeQuery()).thenThrow(new SQLException("settings unavailable"));
        assertThatThrownBy(() -> new MigrationLockTimeoutDataSource(source).getConnection())
                .isInstanceOf(SQLException.class);
        verify(connection).close();
    }
}
