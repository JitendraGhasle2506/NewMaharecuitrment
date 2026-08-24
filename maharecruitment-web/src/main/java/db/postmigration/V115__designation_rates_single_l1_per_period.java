package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V115__designation_rates_single_l1_per_period extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    with ranked_rates as (
                        select
                            rate_id,
                            row_number() over (
                                partition by designation_id, effective_from
                                order by
                                    case when upper(trim(active_flag)) = 'Y' then 0 else 1 end,
                                    case when upper(trim(level_code)) = 'L1' then 0 else 1 end,
                                    case
                                        when upper(trim(level_code)) ~ '^L[0-9]+$'
                                            then substring(upper(trim(level_code)) from 2)::integer
                                        else 2147483647
                                    end,
                                    updated_date_time desc nulls last,
                                    created_date_time desc nulls last,
                                    rate_id
                            ) as row_rank
                        from manpower_designation_rate
                    )
                    delete from manpower_designation_rate as target
                    using ranked_rates as ranked
                    where target.rate_id = ranked.rate_id
                      and ranked.row_rank > 1
                    """);

            statement.execute("""
                    update manpower_designation_rate
                    set level_code = 'L1',
                        updated_date_time = current_timestamp
                    where level_code is distinct from 'L1'
                    """);
        }
    }
}
