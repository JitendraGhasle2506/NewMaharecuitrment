package db.postmigration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V70__auth_agency_monthly_bills_hr_menu_fix extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        new R__auth_reference_data().migrate(context);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(
                new SingleConnectionDataSource(context.getConnection(), true));

        jdbcTemplate.update("""
                delete from menu_role
                where menu_id in (
                    select menu_id from mst_menu
                    where upper(url) = upper('/auditor/agency-monthly-bills')
                )
                """);
        jdbcTemplate.update("""
                update mst_menu
                set is_active = 'N', updated_date = current_timestamp
                where upper(url) = upper('/auditor/agency-monthly-bills')
                """);
    }
}
