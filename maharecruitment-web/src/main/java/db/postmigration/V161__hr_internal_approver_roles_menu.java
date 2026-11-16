package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Lets HR open Admin > Internal Approver Roles: adds a direct sidebar link to the screen for ROLE_HR.
 * The Admin entry seeded by V160 stays as it is. URL access itself is granted in SecurityConfig.
 */
public class V161__hr_internal_approver_roles_menu extends BaseJavaMigration {

    static final String SCREEN_URL = "/admin/internal-approver-roles";

    @Override
    public void migrate(Context context) throws Exception {
        // try (Statement statement = context.getConnection().createStatement()) {
        //     statement.execute("""
        //             insert into mst_menu (menu_name_english, menu_name_marathi, is_active, icon, url, is_sub_menu)
        //             select 'Internal Approver Roles', 'Internal Approver Roles', 'Y', 'fa-solid fa-user-check', '%s', 1
        //             where not exists (select 1 from mst_menu where upper(url) = upper('%s'))
        //             """.formatted(SCREEN_URL, SCREEN_URL));
        //     statement.execute("""
        //             insert into menu_role (menu_id, id)
        //             select m.menu_id, r.id
        //             from mst_menu m
        //             join roles r on upper(r.name) = 'ROLE_HR'
        //             where upper(m.url) = upper('%s')
        //               and not exists (
        //                   select 1 from menu_role mr where mr.menu_id = m.menu_id and mr.id = r.id)
        //             """.formatted(SCREEN_URL));
        // }
    }
}
