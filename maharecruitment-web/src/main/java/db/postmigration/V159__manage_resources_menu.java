package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Seeds the DB-driven "Manage Resources" sidebar menu and its initial role mapping.
 * Runs once; afterwards the roles are maintained through Admin menu management (menu_role).
 */
public class V159__manage_resources_menu extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    insert into mst_menu (menu_name_english, menu_name_marathi, is_active, icon, url, is_sub_menu)
                    select 'Manage Resources', 'Manage Resources', 'Y', 'fa-solid fa-users-gear',
                           '/reporting-manager/resources', 1
                    where not exists (
                        select 1 from mst_menu where upper(url) = upper('/reporting-manager/resources'))
                    """);
            statement.execute("""
                    insert into menu_role (menu_id, id)
                    select m.menu_id, r.id
                    from mst_menu m
                    join roles r on upper(r.name) in ('ROLE_HOD', 'ROLE_COO', 'ROLE_CTO', 'ROLE_CFO')
                    where upper(m.url) = upper('/reporting-manager/resources')
                      and not exists (
                          select 1 from menu_role mr where mr.menu_id = m.menu_id and mr.id = r.id)
                    """);
        }
    }
}
