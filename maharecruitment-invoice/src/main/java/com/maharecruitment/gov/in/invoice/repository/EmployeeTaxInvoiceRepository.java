package com.maharecruitment.gov.in.invoice.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.invoice.dto.EmployeeTaxInvoiceListItem;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceGenerationFilter;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceView;

@Repository
public class EmployeeTaxInvoiceRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public EmployeeTaxInvoiceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Long> findIdByGenerationToken(String token) {
        return jdbc.query("select id from employee_tax_invoice where generation_token = :token",
                Map.of("token", token), (rs, row) -> rs.getLong("id")).stream().findFirst();
    }

    public long save(String token, TaxInvoiceGenerationFilter filter, TaxInvoiceView invoice, String snapshot) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("token", token)
                .addValue("departmentId", filter.getDepartmentId())
                .addValue("subDepartmentId", filter.getSubDepartmentId())
                .addValue("projectId", filter.getProjectId())
                .addValue("periodStart", filter.getStartDate())
                .addValue("periodEnd", filter.getEndDate())
                .addValue("tiNumber", invoice.getTiNumber())
                .addValue("tiDate", invoice.getTiDate())
                .addValue("requestId", invoice.getRequestId())
                .addValue("projectName", invoice.getProjectName())
                .addValue("billedTo", invoice.getBilledTo())
                .addValue("totalAmount", invoice.getTotalAmount())
                .addValue("generatedOn", invoice.getGeneratedOn())
                .addValue("generatedBy", invoice.getGeneratedByLoginId())
                .addValue("snapshot", snapshot);
        // A retried or concurrent submission of the same preview returns the first saved invoice.
        List<Long> inserted = jdbc.query("""
                insert into employee_tax_invoice (generation_token, department_id, sub_department_id, project_id,
                    period_start, period_end, ti_number, ti_date, request_id, project_name, billed_to,
                    total_amount, generated_on, generated_by, invoice_snapshot)
                values (:token, :departmentId, :subDepartmentId, :projectId, :periodStart, :periodEnd,
                    :tiNumber, :tiDate, :requestId, :projectName, :billedTo, :totalAmount,
                    :generatedOn, :generatedBy, :snapshot)
                on conflict (generation_token) do nothing
                returning id
                """, params, (rs, row) -> rs.getLong("id"));
        return inserted.isEmpty() ? findIdByGenerationToken(token).orElseThrow() : inserted.getFirst();
    }

    public Optional<String> findSnapshotById(long id) {
        return jdbc.query("select invoice_snapshot from employee_tax_invoice where id = :id",
                Map.of("id", id), (rs, row) -> rs.getString("invoice_snapshot")).stream().findFirst();
    }

    public Page<EmployeeTaxInvoiceListItem> findAll(String search, Long departmentId, Integer month, Integer year,
            Pageable pageable) {
        List<String> conditions = new ArrayList<>();
        if (!search.isBlank()) {
            conditions.add("""
                    (lower(ti_number) like :search or lower(request_id) like :search
                        or lower(project_name) like :search or lower(billed_to) like :search)""");
        }
        if (departmentId != null) {
            conditions.add("department_id = :departmentId");
        }
        // Month and year filter on the date the invoice was generated, not the billing period.
        if (month != null) {
            conditions.add("extract(month from generated_on) = :month");
        }
        if (year != null) {
            conditions.add("extract(year from generated_on) = :year");
        }
        String where = conditions.isEmpty() ? "" : " where " + String.join(" and ", conditions) + " ";
        String escapedSearch = search.toLowerCase(java.util.Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        MapSqlParameterSource params = new MapSqlParameterSource("search", "%" + escapedSearch + "%")
                .addValue("departmentId", departmentId).addValue("month", month).addValue("year", year)
                .addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        Long count = jdbc.queryForObject("select count(*) from employee_tax_invoice" + where, params, Long.class);
        // List only summary columns; large employee snapshots are loaded only when opening an invoice.
        List<EmployeeTaxInvoiceListItem> rows = jdbc.query("""
                select id, ti_number, ti_date, request_id, project_name, billed_to, period_start, period_end,
                    total_amount, generated_on from employee_tax_invoice
                """ + where + " order by generated_on desc, id desc limit :limit offset :offset", params,
                (rs, row) -> new EmployeeTaxInvoiceListItem(rs.getLong("id"), rs.getString("ti_number"),
                        rs.getDate("ti_date").toLocalDate(), rs.getString("request_id"), rs.getString("project_name"),
                        rs.getString("billed_to"), rs.getDate("period_start").toLocalDate(),
                        rs.getDate("period_end").toLocalDate(), rs.getBigDecimal("total_amount"),
                        rs.getTimestamp("generated_on").toLocalDateTime()));
        return new PageImpl<>(rows, pageable, count == null ? 0 : count);
    }
}
