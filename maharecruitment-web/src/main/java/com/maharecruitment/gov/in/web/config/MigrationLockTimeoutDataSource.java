package com.maharecruitment.gov.in.web.config;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DelegatingDataSource;

/** Limits migration connections and restores pooled session settings before returning them. */
final class MigrationLockTimeoutDataSource extends DelegatingDataSource {
    MigrationLockTimeoutDataSource(DataSource target) { super(target); }

    @Override
    public Connection getConnection() throws SQLException { return configure(super.getConnection()); }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return configure(super.getConnection(username, password));
    }

    private Connection configure(Connection connection) throws SQLException {
        try {
            if (!connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgresql")) {
                return connection;
            }
            boolean originalAutoCommit = connection.getAutoCommit();
            if (!originalAutoCommit) connection.setAutoCommit(true);
            String previous;
            try (PreparedStatement statement = connection.prepareStatement(
                    "select current_setting('lock_timeout')")) {
                statement.setQueryTimeout(5);
                try (ResultSet rows = statement.executeQuery()) { rows.next(); previous = rows.getString(1); }
            }
            try (PreparedStatement statement = connection.prepareStatement("select set_config('lock_timeout', '5s', false)")) {
                statement.setQueryTimeout(5);
                statement.execute();
            }
            if (!originalAutoCommit) connection.setAutoCommit(false);
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("close")) {
                            if (connection.isClosed()) return null;
                            try {
                                if (!connection.getAutoCommit()) { connection.rollback(); connection.setAutoCommit(true); }
                                try (PreparedStatement reset = connection.prepareStatement("select set_config('lock_timeout', ?, false)")) {
                                    reset.setQueryTimeout(5);
                                    reset.setString(1, previous);
                                    reset.execute();
                                }
                                if (!originalAutoCommit) connection.setAutoCommit(false);
                            } catch (SQLException failure) {
                                try { connection.abort(Runnable::run); } catch (SQLException abortFailure) { failure.addSuppressed(abortFailure); }
                                throw failure;
                            } finally { connection.close(); }
                            return null;
                        }
                        try { return method.invoke(connection, args); }
                        catch (InvocationTargetException failure) { throw failure.getCause(); }
                    });
        } catch (SQLException failure) {
            try { connection.abort(Runnable::run); } catch (SQLException abortFailure) { failure.addSuppressed(abortFailure); }
            try { connection.close(); } catch (SQLException closeFailure) { failure.addSuppressed(closeFailure); }
            throw failure;
        }
    }
}
