package db.postmigration;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class R__department_and_recruitment_reference_data extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));

        if (tableExists(connection, "department_tax_rate_master")) {
            seedTaxRates(jdbcTemplate);
        }
        if (tableExists(connection, "rank_release_rule")) {
            seedRankReleaseRules(jdbcTemplate);
        }
    }

    private void seedTaxRates(JdbcTemplate jdbcTemplate) {
        List<TaxRateSeed> taxRates = List.of(
                new TaxRateSeed("SGST", "State Goods and Services Tax", "9.0000"),
                new TaxRateSeed("CGST", "Central Goods and Services Tax", "9.0000"));

        for (TaxRateSeed taxRate : taxRates) {
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from department_tax_rate_master where upper(tax_code) = upper(?) and effective_from = ?",
                    Integer.class,
                    taxRate.taxCode(),
                    Date.valueOf(LocalDate.of(2025, 4, 1)));
            if (count != null && count == 0) {
                jdbcTemplate.update(
                        "insert into department_tax_rate_master (tax_code, tax_name, rate_percentage, effective_from, effective_to, active) "
                                + "values (?, ?, ?, ?, ?, ?)",
                        taxRate.taxCode(),
                        taxRate.taxName(),
                        new java.math.BigDecimal(taxRate.ratePercentage()),
                        Date.valueOf(LocalDate.of(2025, 4, 1)),
                        Date.valueOf(LocalDate.of(2026, 3, 31)),
                        Boolean.TRUE);
            }
        }
    }

    private void seedRankReleaseRules(JdbcTemplate jdbcTemplate) {
        List<RankRuleSeed> rules = List.of(
                new RankRuleSeed(1, 0),
                new RankRuleSeed(2, 3),
                new RankRuleSeed(3, 3));

        for (RankRuleSeed rule : rules) {
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from rank_release_rule where rank_number = ?",
                    Integer.class,
                    rule.rankNumber());
            if (count != null && count == 0) {
                jdbcTemplate.update(
                        "insert into rank_release_rule "
                                + "(rank_release_rule_id, rank_number, release_after_days, delay_from_previous_rank_days, effective_from, effective_to, is_active, created_date_time, updated_date_time) "
                                + "values (nextval('rank_release_rule_seq'), ?, ?, ?, current_date, ?, ?, current_timestamp, current_timestamp)",
                        rule.rankNumber(),
                        rule.releaseAfterDays(),
                        rule.releaseAfterDays(),
                        Date.valueOf(LocalDate.of(9999, 12, 31)),
                        Boolean.TRUE);
            }
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }

    private record TaxRateSeed(String taxCode, String taxName, String ratePercentage) {
    }

    private record RankRuleSeed(int rankNumber, int releaseAfterDays) {
    }
}
