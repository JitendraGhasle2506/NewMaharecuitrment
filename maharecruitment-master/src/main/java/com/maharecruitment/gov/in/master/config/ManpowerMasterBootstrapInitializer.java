package com.maharecruitment.gov.in.master.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.List;

@Component
@Order(12)
@RequiredArgsConstructor
@Slf4j
public class ManpowerMasterBootstrapInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Starting Manpower Master data bootstrap using JdbcTemplate...");

        bootstrapResourceLevels();
        bootstrapDesignations();
        bootstrapDesignationRates();

        log.info("Manpower Master data bootstrap completed.");
    }

    private void bootstrapResourceLevels() {
        List<ResourceLevelData> data = List.of(
                new ResourceLevelData(1L, "L1", "level 1", 0.60, 3.00),
                new ResourceLevelData(2L, "L2", "level 2", 3.00, 6.00),
                new ResourceLevelData(3L, "L3", "level 3", 6.00, 9.00),
                new ResourceLevelData(4L, "L4", "level 4", 9.00, 12.00),
                new ResourceLevelData(5L, "L5", "level 5", 12.00, 15.00),
                new ResourceLevelData(6L, "L5-STM", "level L5-STM", 15.00, 18.00));

        for (ResourceLevelData item : data) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM resource_level_experience_mst WHERE level_id = ?",
                    Integer.class, item.id());

            if (count != null && count == 0) {
                jdbcTemplate.update(
                        "INSERT INTO resource_level_experience_mst (level_id, level_code, level_name, min_experience, max_experience, active_flag, created_date_time) VALUES (?, ?, ?, ?, ?, 'Y', CURRENT_TIMESTAMP)",
                        item.id(), item.code(), item.name(), item.minExp(), item.maxExp());
                log.info("Bootstrapped resource level: {} - {}", item.code(), item.name());
            }
        }
    }

    private void bootstrapDesignations() {
        List<DesignationData> data = List.of(
                new DesignationData(1L, "Technical", "Solution Architect – Java",
                        "BTech / BE / MCA in\n(Computer Science /\nIT / EE)",
                        "AWS Certified Solutions Architect / TOGAF Certification or equivalent certifications"),
                new DesignationData(2L, "Technical", "Solution Architect – .Net",
                        "BTech / BE / MCA in\n(Computer Science /\nIT / EE)",
                        "Microsoft Certified: Azure Solutions Architect Expert / TOGAF Certification or equivalent certifications"),
                new DesignationData(3L, "Technical", "Quality Manager",
                        "BTech / BE / MCA in\n(Computer Science /\nIT / EE)",
                        "ISO 9001 Lead Auditor / Lean Six Sigma Green Belt Certification or equivalent certification"),
                new DesignationData(4L, "Technical", "QA Lead", "BTech / BE / MCA in\n(Computer Science /\nIT / EE)",
                        "iSQI CAT or equivalent Certification"));

        for (DesignationData item : data) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM manpower_designation_master WHERE designation_id = ?",
                    Integer.class, item.id());

            if (count != null && count == 0) {
                jdbcTemplate.update(
                        "INSERT INTO manpower_designation_master (designation_id, category, designation_name, educational_qualification, certification, role_name, active_flag, created_date_time) VALUES (?, ?, ?, ?, ?, '', 'Y', CURRENT_TIMESTAMP)",
                        item.id(), item.category(), item.name(), item.qualification(), item.certification());
                log.info("Bootstrapped designation: {}", item.name());
            }
        }
    }

    private void bootstrapDesignationRates() {
        List<RateData> data = List.of(
                new RateData(1L, 1L, "L4", 320000.00, "2026-03-01", "2026-12-31"),
                new RateData(2L, 1L, "L5", 350000.00, "2026-03-01", "2026-12-31"),
                new RateData(3L, 2L, "L4", 320000.00, "2026-03-01", "2026-12-31"),
                new RateData(4L, 2L, "L5", 350000.00, "2026-03-01", "2026-12-31"),
                new RateData(5L, 3L, "L5", 160000.00, "2026-03-01", "2026-12-31"),
                new RateData(6L, 4L, "L4", 135000.00, "2026-03-01", "2026-12-31"));

        for (RateData item : data) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM manpower_designation_rate WHERE rate_id = ?",
                    Integer.class, item.id());

            if (count != null && count == 0) {
                jdbcTemplate.update(
                        "INSERT INTO manpower_designation_rate (rate_id, designation_id, level_code, gross_monthly_ctc, effective_from, effective_to, active_flag, created_date_time) VALUES (?, ?, ?, ?, ?, ?, 'Y', CURRENT_TIMESTAMP)",
                        item.id(), item.designationId(), item.levelCode(), item.ctc(), Date.valueOf(item.from()),
                        Date.valueOf(item.to()));
                log.info("Bootstrapped rate: id={}, designation={}", item.id(), item.designationId());
            }
        }
    }

    private record ResourceLevelData(Long id, String code, String name, double minExp, double maxExp) {
    }

    private record DesignationData(Long id, String category, String name, String qualification, String certification) {
    }

    private record RateData(Long id, Long designationId, String levelCode, double ctc, String from, String to) {
    }
}
