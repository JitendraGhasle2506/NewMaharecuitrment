package db.postmigration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmployeeMasterOnboardingDateSchemaSupportTest {
    private final Connection connection = mock(Connection.class);
    private final DatabaseMetaData metadata = mock(DatabaseMetaData.class);
    private final PreparedStatement inspection = mock(PreparedStatement.class);
    private final ResultSet columns = mock(ResultSet.class);
    private final Statement migration = mock(Statement.class);

    @BeforeEach
    void setUp() throws Exception {
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.prepareStatement(anyString())).thenReturn(inspection);
        when(inspection.executeQuery()).thenReturn(columns);
        when(connection.createStatement()).thenReturn(migration);
        when(connection.getAutoCommit()).thenReturn(true);
        when(columns.getString("nspname")).thenReturn("public");
    }

    @Test
    void migratesLegacyColumnAndBackfillsBeforeEnforcingConstraint() throws Exception {
        when(columns.next()).thenReturn(true, true, false);
        when(columns.getString("attname")).thenReturn("onboarding_date", "joining_date");
        assertThat(EmployeeMasterOnboardingDateSchemaSupport.apply(connection)).isTrue();
        var order = inOrder(migration, connection);
        order.verify(migration).execute("alter table \"public\".\"employee_master\" rename column onboarding_date to mahait_onboarding_date");
        order.verify(migration).execute("update \"public\".\"employee_master\" set mahait_onboarding_date = joining_date where mahait_onboarding_date is null and joining_date is not null");
        order.verify(migration).execute("update \"public\".\"employee_master\" set mahait_onboarding_date = current_date where mahait_onboarding_date is null");
        order.verify(migration).execute("alter table \"public\".\"employee_master\" alter column mahait_onboarding_date set not null");
        order.verify(connection).commit();
    }

    @Test
    void missingTableIsLeftToTheBaselineMigration() throws Exception {
        when(columns.next()).thenReturn(false);
        assertThat(EmployeeMasterOnboardingDateSchemaSupport.apply(connection)).isFalse();
        verify(connection, never()).createStatement();
    }

    @Test
    void completedSchemaDoesNotAcquireAnAlterationLockOrRunUpdates() throws Exception {
        when(columns.next()).thenReturn(true, true, false);
        when(columns.getString("attname")).thenReturn("joining_date", "mahait_onboarding_date");
        when(columns.getBoolean("attnotnull")).thenReturn(true);
        assertThat(EmployeeMasterOnboardingDateSchemaSupport.apply(connection)).isFalse();
        verify(connection, never()).createStatement();
        verify(connection, never()).setAutoCommit(false);
        verify(metadata, never()).getColumns(any(), any(), any(), any());
        verify(inspection).setQueryTimeout(5);
    }

    @Test
    void repairsNullableDateWithBoundedLockWaitAndCommitsItsOwnTransaction() throws Exception {
        nullableColumns();
        assertThat(EmployeeMasterOnboardingDateSchemaSupport.apply(connection)).isTrue();
        var order = inOrder(connection, migration);
        order.verify(connection).setAutoCommit(false);
        order.verify(migration).execute("set local lock_timeout = '5s'");
        order.verify(migration).execute("update \"public\".\"employee_master\" set mahait_onboarding_date = joining_date where mahait_onboarding_date is null and joining_date is not null");
        order.verify(migration).execute("update \"public\".\"employee_master\" set mahait_onboarding_date = current_date where mahait_onboarding_date is null");
        order.verify(migration).execute("alter table \"public\".\"employee_master\" alter column mahait_onboarding_date set not null");
        order.verify(connection).commit();
        order.verify(connection).setAutoCommit(true);
    }

    @Test
    void failedAlterationRollsBackInsteadOfLeavingAnOpenTransaction() throws Exception {
        nullableColumns();
        when(migration.execute("alter table \"public\".\"employee_master\" alter column mahait_onboarding_date set not null"))
                .thenThrow(new SQLException("lock timeout", "55P03"));
        assertThatThrownBy(() -> EmployeeMasterOnboardingDateSchemaSupport.apply(connection))
                .isInstanceOf(SQLException.class);
        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(connection).setAutoCommit(true);
    }

    @Test
    void rollbackFailureDiscardsTheConnectionWithoutImplicitlyCommitting() throws Exception {
        nullableColumns();
        when(migration.execute("set local lock_timeout = '5s'")).thenThrow(new SQLException("migration failed"));
        doThrow(new SQLException("rollback failed")).when(connection).rollback();
        assertThatThrownBy(() -> EmployeeMasterOnboardingDateSchemaSupport.apply(connection))
                .isInstanceOf(SQLException.class).hasMessage("migration failed");
        verify(connection).abort(any());
        verify(connection, never()).setAutoCommit(true);
        verify(connection, never()).commit();
    }

    @Test
    void migrationLeavesCommitAndRollbackToFlywayWhenItOwnsTheTransaction() throws Exception {
        nullableColumns();
        when(connection.getAutoCommit()).thenReturn(false);
        assertThat(EmployeeMasterOnboardingDateSchemaSupport.apply(connection)).isTrue();
        verify(connection, never()).commit();
        verify(connection, never()).rollback();
        verify(connection, never()).setAutoCommit(anyBoolean());
    }

    @Test
    void failedInspectionDoesNotPretendTheColumnIsMissing() throws Exception {
        when(inspection.executeQuery()).thenThrow(new SQLException("catalog unavailable"));
        assertThatThrownBy(() -> EmployeeMasterOnboardingDateSchemaSupport.apply(connection))
                .isInstanceOf(SQLException.class);
        verify(connection, never()).createStatement();
    }

    private void nullableColumns() throws SQLException {
        when(columns.next()).thenReturn(true, true, false);
        when(columns.getString("attname")).thenReturn("joining_date", "mahait_onboarding_date");
        when(columns.getBoolean("attnotnull")).thenReturn(false);
    }
}
