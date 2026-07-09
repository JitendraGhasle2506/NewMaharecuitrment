package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V93__mobile_refresh_token_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    create table if not exists mobile_refresh_token (
                        mobile_refresh_token_id bigserial primary key,
                        user_id bigint not null,
                        token_hash varchar(64) not null,
                        issued_at timestamp not null,
                        expires_at timestamp not null,
                        revoked_at timestamp,
                        replaced_by_token_hash varchar(64),
                        created_at timestamp not null default current_timestamp,
                        updated_at timestamp not null default current_timestamp,
                        version bigint not null default 0,
                        constraint uk_mobile_refresh_token_hash unique (token_hash),
                        constraint fk_mobile_refresh_token_user
                            foreign key (user_id) references users(id) on delete cascade
                    )
                    """);

            statement.execute("""
                    create index if not exists idx_mobile_refresh_token_user
                    on mobile_refresh_token (user_id)
                    """);

            statement.execute("""
                    create index if not exists idx_mobile_refresh_token_expiry
                    on mobile_refresh_token (expires_at)
                    """);

            statement.execute("""
                    create index if not exists idx_mobile_refresh_token_revoked
                    on mobile_refresh_token (revoked_at)
                    """);
        }
    }
}
