package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** New audit events capture IPs; historical addresses cannot be reconstructed. */
public class V155__audit_client_ip_support extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            for (String table : new String[]{"audit_event", "agency_master_audit_log", "commission_rate_audit_log",
                    "mahait_profile_master_audit_log", "team_management_audit_log", "employee_type_conversion_audit",
                    "employee_cell_mapping_audit_log", "employee_location_mapping_audit_log",
                    "login_logout_audit_history", "asset_audit_log"}) {
                statement.execute("alter table if exists " + table + " add column if not exists ip_address varchar(64)");
            }
        }
    }
}
