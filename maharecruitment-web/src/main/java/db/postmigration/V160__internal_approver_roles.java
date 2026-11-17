package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Moves the roles allowed to decide INTERNAL employees' leave/tour requests from code to the database,
 * adds their change log, and seeds the Admin screen that maintains them.
 * The seed equals the previous fixed rule, so behaviour is unchanged until an administrator edits it.
 */
public class V160__internal_approver_roles extends BaseJavaMigration {

    static final String SEED_ROLES = "'ROLE_PM', 'ROLE_STM', 'ROLE_COO', 'ROLE_HOD', 'ROLE_MD', 'ROLE_CTO', 'ROLE_CFO'";
    static final String SCREEN_URL = "/admin/internal-approver-roles";

    @Override
    public void migrate(Context context) throws Exception {
        // try (Statement statement = context.getConnection().createStatement()) {
        //     statement.execute("""
        //             create table if not exists internal_approver_role (
        //                 role_id bigint primary key,
        //                 granted_by_user_id bigint,
        //                 granted_at timestamp not null default current_timestamp
        //             )
        //             """);
        //     statement.execute("alter table internal_approver_role drop constraint if exists fk_internal_approver_role_role");
        //     statement.execute("""
        //             alter table internal_approver_role add constraint fk_internal_approver_role_role
        //             foreign key (role_id) references roles (id) on delete cascade
        //             """);
        //     statement.execute("""
        //             create table if not exists internal_approver_role_audit_log (
        //                 audit_id bigserial primary key,
        //                 action_type varchar(20) not null,
        //                 roles_added varchar(500),
        //                 roles_removed varchar(500),
        //                 roles_before varchar(1000),
        //                 roles_after varchar(1000) not null,
        //                 remarks varchar(500),
        //                 actor_user_id bigint,
        //                 actor_name varchar(150),
        //                 actor_login_id varchar(150),
        //                 ip_address varchar(64) not null,
        //                 user_agent varchar(500),
        //                 changed_at timestamp not null default current_timestamp
        //             )
        //             """);
        //     statement.execute("""
        //             create index if not exists idx_internal_approver_role_audit_changed
        //             on internal_approver_role_audit_log (changed_at desc, audit_id desc)
        //             """);
        //     // Hibernate may have created both tables first without column defaults: set timestamps explicitly.
        //     statement.execute("""
        //             insert into internal_approver_role (role_id, granted_at)
        //             select r.id, current_timestamp from roles r
        //             where upper(r.name) in (%s)
        //               and not exists (select 1 from internal_approver_role)
        //             """.formatted(SEED_ROLES));
        //     statement.execute("""
        //             insert into internal_approver_role_audit_log
        //                 (action_type, roles_after, remarks, actor_name, ip_address, changed_at)
        //             select 'SEEDED', string_agg(r.name, ',' order by r.name),
        //                    'Initial roles carried over from the previous fixed rule', 'System', 'SYSTEM',
        //                    current_timestamp
        //             from internal_approver_role a
        //             join roles r on r.id = a.role_id
        //             having count(*) > 0
        //                and not exists (select 1 from internal_approver_role_audit_log)
        //             """);
        //     statement.execute("""
        //             insert into sub_menu_mst (sub_menu_id, sub_menu_name_english, sub_menu_name_marathi, url, icon, is_active, menu_id)
        //             select nextval('mstsubmenu_seq'), 'Internal Approver Roles', 'Internal Approver Roles', '%s',
        //                    'fa-solid fa-user-check', 'Y', s.menu_id
        //             from sub_menu_mst s
        //             where upper(s.url) = upper('/admin/roles')
        //               and not exists (select 1 from sub_menu_mst x where upper(x.url) = upper('%s'))
        //             fetch first 1 row only
        //             """.formatted(SCREEN_URL, SCREEN_URL));
        //     statement.execute("""
        //             insert into sub_menu_role (sub_menu_id, id)
        //             select s.sub_menu_id, r.id
        //             from sub_menu_mst s
        //             join roles r on upper(r.name) = 'ROLE_ADMIN'
        //             where upper(s.url) = upper('%s')
        //               and not exists (
        //                   select 1 from sub_menu_role x where x.sub_menu_id = s.sub_menu_id and x.id = r.id)
        //             """.formatted(SCREEN_URL));
        // }
    }
}
