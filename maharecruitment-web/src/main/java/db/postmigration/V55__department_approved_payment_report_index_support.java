package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V55__department_approved_payment_report_index_support extends BaseJavaMigration {

    private static final String TABLE_NAME = "department_advance_payment";
    private static final String STATUS_UPDATED_INDEX = "idx_dep_adv_pay_status_updated_id";
    private static final String STATUS_DEPT_UPDATED_INDEX = "idx_dep_adv_pay_status_dept_updated_id";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection) || !tableExists(connection, TABLE_NAME)) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));

        if (!indexExists(connection, TABLE_NAME, STATUS_UPDATED_INDEX)) {
            jdbcTemplate.execute("""
                    create index idx_dep_adv_pay_status_updated_id
                    on department_advance_payment (application_status, updated_date desc, id desc)
                    """);
        }

        if (!indexExists(connection, TABLE_NAME, STATUS_DEPT_UPDATED_INDEX)) {
            jdbcTemplate.execute("""
                    create index idx_dep_adv_pay_status_dept_updated_id
                    on department_advance_payment (application_status, department_registration_id, updated_date desc, id desc)
                    """);
        }
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

    private boolean indexExists(Connection connection, String tableName, String indexName) {
        try (ResultSet rs = connection.getMetaData().getIndexInfo(null, null, tableName, false, false)) {
            while (rs.next()) {
                if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        } catch (SQLException ex) {
            return false;
        }

        try (ResultSet rs = connection.getMetaData().getIndexInfo(
                null,
                null,
                tableName.toUpperCase(Locale.ROOT),
                false,
                false)) {
            while (rs.next()) {
                if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
                    return true;
                }
            }
            return false;
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
