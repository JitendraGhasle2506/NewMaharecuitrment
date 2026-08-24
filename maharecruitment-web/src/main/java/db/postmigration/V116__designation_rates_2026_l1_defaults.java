package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V116__designation_rates_2026_l1_defaults extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    select setval(
                        pg_get_serial_sequence('manpower_designation_rate', 'rate_id'),
                        coalesce(max(rate_id), 0) + 1,
                        false
                    )
                    from manpower_designation_rate
                    """);

            statement.execute("""
                    insert into manpower_designation_rate (
                        designation_id,
                        level_code,
                        gross_monthly_ctc,
                        effective_from,
                        effective_to,
                        active_flag,
                        created_date_time
                    )
                    select
                        designation.designation_id,
                        'L1',
                        25000 + (designation.designation_id * 1000),
                        date '2026-01-01',
                        date '2026-12-31',
                        'Y',
                        current_timestamp
                    from manpower_designation_master as designation
                    where designation.designation_id between 1 and 89
                      and not exists (
                          select 1
                          from manpower_designation_rate as existing_rate
                          where existing_rate.designation_id = designation.designation_id
                            and upper(trim(existing_rate.level_code)) = 'L1'
                            and existing_rate.effective_from = date '2026-01-01'
                      )
                    on conflict (designation_id, level_code, effective_from) do nothing
                    """);
        }
    }
}
