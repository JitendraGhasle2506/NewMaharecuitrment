package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V88__pre_onboarding_photo_embedding_support extends BaseJavaMigration {

    private static final String TABLE_NAME = "agency_candidate_pre_onboarding";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection) || !tableExists(connection, TABLE_NAME)) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        jdbcTemplate.execute("""
                alter table agency_candidate_pre_onboarding
                add column if not exists embedding text
                """);
    }

    private boolean tableExists(Connection connection, String tableName) {
        try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null)) {
            if (rs.next()) {
                return true;
            }
        } catch (SQLException ex) {
            return false;
        }

        try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName.toUpperCase(Locale.ROOT), null)) {
            return rs.next();
        } catch (SQLException ex) {
            return false;
        }
    }

    private boolean isPostgreSql(Connection connection) {
        try {
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            return databaseProductName != null
                    && databaseProductName.trim().toLowerCase(Locale.ROOT).contains("postgresql");
        } catch (SQLException ex) {
            return false;
        }
    }
}
