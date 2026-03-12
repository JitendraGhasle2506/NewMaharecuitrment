package com.maharecruitment.gov.in.master.config;

import java.sql.Date;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
                                new ResourceLevelData(6L, "L5-STM", "level L5-STM", 15.00, 18.00),
                                new ResourceLevelData(7L, "L6", "level 6", 18.00, 21.00),
                                new ResourceLevelData(8L, "L7", "level 7", 21.00, 25.00));

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

                                // ===== Technical =====

                                new DesignationData(
                                                1L,
                                                "Technical",
                                                "Solution Architect – Java",
                                                "BTech / BE / MCA (CS / IT / EE)",
                                                "AWS Certified Solutions Architect / TOGAF",
                                                List.of("L4", "L5")),

                                new DesignationData(
                                                2L,
                                                "Technical",
                                                "Solution Architect – .Net",
                                                "BTech / BE / MCA (CS / IT / EE)",
                                                "Azure Solutions Architect Expert / TOGAF",
                                                List.of("L4", "L5")),

                                new DesignationData(
                                                3L,
                                                "Technical",
                                                "Quality Manager",
                                                "BTech / BE / MCA",
                                                "ISO 9001 Lead Auditor / Lean Six Sigma",
                                                List.of("L5")),

                                new DesignationData(
                                                4L,
                                                "Technical",
                                                "QA Lead",
                                                "BTech / BE / MCA",
                                                "iSQI CAT",
                                                List.of("L4")),

                                new DesignationData(
                                                5L,
                                                "Technical",
                                                "Java Developer",
                                                "BTech / BE / MCA",
                                                "Role based certification",
                                                List.of("L2", "L3", "L4")),

                                new DesignationData(
                                                6L,
                                                "Technical",
                                                ".Net Developer",
                                                "BTech / BE / MCA",
                                                "Role based certification",
                                                List.of("L2", "L3", "L4")),

                                new DesignationData(
                                                7L,
                                                "Technical",
                                                "PHP Developer",
                                                "BTech / BE / MCA",
                                                "Role based certification",
                                                List.of("L2", "L3", "L4")),

                                new DesignationData(
                                                8L,
                                                "Technical",
                                                "Full Stack Developer",
                                                "BTech / BE / MCA",
                                                "MEAN / MERN Certification",
                                                List.of("L3", "L4")),

                                new DesignationData(
                                                9L,
                                                "Technical",
                                                "DB Developer",
                                                "BTech / BE / MCA",
                                                "OCA/OCP/MCDBA/Azure DB",
                                                List.of("L2", "L3", "L4")),

                                new DesignationData(
                                                10L,
                                                "Technical",
                                                "GIS Developer",
                                                "BTech / BE GIS / CS",
                                                "GIS Certification",
                                                List.of("L2", "L3")),

                                new DesignationData(
                                                11L,
                                                "Technical",
                                                "Python Developer",
                                                "BTech / BE / MCA",
                                                "Role based certification",
                                                List.of("L2", "L3")),

                                new DesignationData(
                                                12L,
                                                "Technical",
                                                "AI & ML Developer",
                                                "BTech / BE / MCA",
                                                "AI / ML Certification",
                                                List.of("L1", "L2", "L3", "L4")),

                                new DesignationData(
                                                13L,
                                                "Technical",
                                                "Blockchain Developer",
                                                "BTech / BE / MCA",
                                                "Blockchain Certification",
                                                List.of("L1", "L2", "L3")),

                                new DesignationData(
                                                14L,
                                                "Technical",
                                                "Cloud Expert",
                                                "BTech / BE / MCA",
                                                "AWS / Azure Architect",
                                                List.of("L3", "L4")),

                                new DesignationData(
                                                15L,
                                                "Technical",
                                                "Mobile App Developer Android",
                                                "BTech / BE / MCA",
                                                "Google Android Certification",
                                                List.of("L2", "L3")),

                                new DesignationData(
                                                16L,
                                                "Technical",
                                                "Mobile App Developer iOS",
                                                "BTech / BE / MCA",
                                                "iOS Certification",
                                                List.of("L2", "L3")),

                                new DesignationData(
                                                17L,
                                                "Technical",
                                                "AR/VR Developer",
                                                "BTech / BE / MCA",
                                                "Unity Certification",
                                                List.of("L2", "L3", "L4")),

                                new DesignationData(
                                                18L,
                                                "Technical",
                                                "Cloud Security Engineer",
                                                "BTech / BE / MCA",
                                                "CCSP Certification",
                                                List.of("L3")),

                                new DesignationData(
                                                19L,
                                                "Technical",
                                                "System Analyst",
                                                "BTech / BE / MCA",
                                                "ITIL Certification",
                                                List.of("L2", "L3")),

                                // ===== Techno Functional =====

                                new DesignationData(
                                                20L,
                                                "Techno Functional",
                                                "Senior Technical Manager (STM)",
                                                "BTech / BE / MCA",
                                                "PMP / PRINCE2 / CSM",
                                                List.of("L5", "L6", "L7")),

                                new DesignationData(
                                                21L,
                                                "Techno Functional",
                                                "Program Lead – IT Solutions & Delivery",
                                                "BTech / BE / MCA",
                                                "PMP / PRINCE2",
                                                List.of("L3", "L4", "L5")),

                                new DesignationData(
                                                22L,
                                                "Techno Functional",
                                                "Project Manager",
                                                "BTech / BE / MCA",
                                                "PMP",
                                                List.of("L4")),

                                new DesignationData(
                                                23L,
                                                "Techno Functional",
                                                "Project Lead",
                                                "BTech / BE / MCA",
                                                "",
                                                List.of("L3")),

                                new DesignationData(
                                                24L,
                                                "Techno Functional",
                                                "Business Analyst",
                                                "BTech / BE / MCA",
                                                "",
                                                List.of("L2", "L3", "L4")),

                                new DesignationData(
                                                25L,
                                                "Techno Functional",
                                                "Software Tester",
                                                "BTech / BE / MCA",
                                                "ISTQB",
                                                List.of("L1", "L2", "L3")),

                                new DesignationData(
                                                26L,
                                                "Techno Functional",
                                                "Support Engineer",
                                                "BTech / BE / MCA",
                                                "CompTIA A+",
                                                List.of("L1", "L2", "L3")),

                                new DesignationData(
                                                27L,
                                                "Techno Functional",
                                                "Network Admin",
                                                "BTech / BE / MCA",
                                                "CCNA",
                                                List.of("L2", "L3")),

                                new DesignationData(
                                                28L,
                                                "Techno Functional",
                                                "System Administrator",
                                                "BTech / BE / MCA",
                                                "Azure Administrator",
                                                List.of("L2", "L3")),

                                // ===== Operation & Project Management =====

                                new DesignationData(
                                                29L,
                                                "Operation & Project Management",
                                                "Training Expert",
                                                "BTech / BE / MCA",
                                                "Training Certification",
                                                List.of("L3", "L4")),

                                new DesignationData(
                                                30L,
                                                "Operation & Project Management",
                                                "Content Writer / Researcher",
                                                "BA / Journalism",
                                                "",
                                                List.of("L2")),

                                new DesignationData(
                                                31L,
                                                "Operation & Project Management",
                                                "Content Writing Manager",
                                                "BA / Journalism",
                                                "",
                                                List.of("L3")),

                                new DesignationData(
                                                32L,
                                                "Operation & Project Management",
                                                "Social Media Expert",
                                                "Marketing",
                                                "",
                                                List.of("L1", "L2")),

                                new DesignationData(
                                                33L,
                                                "Operation & Project Management",
                                                "PMO / MIS Executive",
                                                "BBA / Finance",
                                                "",
                                                List.of("L2")),

                                new DesignationData(
                                                34L,
                                                "Operation & Project Management",
                                                "PMO / MIS Analyst",
                                                "BBA / Finance",
                                                "",
                                                List.of("L3")),

                                // ===== Support =====

                                new DesignationData(
                                                35L,
                                                "Support",
                                                "Helpdesk Support",
                                                "Graduate",
                                                "ITIL",
                                                List.of("L1", "L2")),

                                new DesignationData(
                                                36L,
                                                "Support",
                                                "HR / Admin",
                                                "MBA HR",
                                                "",
                                                List.of("L2", "L3", "L4")),

                                new DesignationData(
                                                37L,
                                                "Support",
                                                "IT Office Assistant",
                                                "Graduate",
                                                "MSCIT",
                                                List.of("L1", "L2")),

                                new DesignationData(
                                                38L,
                                                "Support",
                                                "Data Entry Operator",
                                                "Graduate",
                                                "MSCIT",
                                                List.of("L1", "L2")));

                for (DesignationData item : data) {

                        Integer count = jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM manpower_designation_master WHERE designation_id = ?",
                                        Integer.class,
                                        item.id());

                        if (count != null && count == 0) {

                                jdbcTemplate.update(
                                                """
                                                                INSERT INTO manpower_designation_master
                                                                (designation_id, category, designation_name,
                                                                 educational_qualification, certification,
                                                                 role_name, active_flag, created_date_time)
                                                                VALUES (?, ?, ?, ?, ?, '', 'Y', CURRENT_TIMESTAMP)
                                                                """,
                                                item.id(),
                                                item.category(),
                                                item.name(),
                                                item.qualification(),
                                                item.certification());

                                log.info("Bootstrapped designation: {}", item.name());
                        }

                        // Insert level mappings
                        for (String level : item.levels()) {

                                Integer mapCount = jdbcTemplate.queryForObject(
                                                "SELECT COUNT(*) FROM designation_level_map WHERE designation_id = ? AND level_code = ?",
                                                Integer.class,
                                                item.id(),
                                                level);

                                if (mapCount != null && mapCount == 0) {

                                        jdbcTemplate.update(
                                                        "INSERT INTO designation_level_map (designation_id, level_code) VALUES (?, ?)",
                                                        item.id(),
                                                        level);

                                        log.info("Mapped designation {} -> level {}", item.name(), level);
                                }
                        }
                }
        }

        private void bootstrapDesignationRates() {

                List<RateData> data = List.of(

                                new RateData(1L, 1L, "L4", 320000, "2026-03-01", "2026-12-31"),
                                new RateData(2L, 1L, "L5", 350000, "2026-03-01", "2026-12-31"),

                                new RateData(3L, 2L, "L4", 320000, "2026-03-01", "2026-12-31"),
                                new RateData(4L, 2L, "L5", 350000, "2026-03-01", "2026-12-31"),

                                new RateData(5L, 3L, "L5", 160000, "2026-03-01", "2026-12-31"),
                                new RateData(6L, 4L, "L4", 135000, "2026-03-01", "2026-12-31"),

                                new RateData(7L, 5L, "L2", 75000, "2026-03-01", "2026-12-31"),
                                new RateData(8L, 5L, "L3", 110000, "2026-03-01", "2026-12-31"),
                                new RateData(9L, 5L, "L4", 140000, "2026-03-01", "2026-12-31"),

                                new RateData(10L, 6L, "L2", 75000, "2026-03-01", "2026-12-31"),
                                new RateData(11L, 6L, "L3", 120000, "2026-03-01", "2026-12-31"),
                                new RateData(12L, 6L, "L4", 150000, "2026-03-01", "2026-12-31"),

                                new RateData(13L, 7L, "L2", 85000, "2026-03-01", "2026-12-31"),
                                new RateData(14L, 7L, "L3", 130000, "2026-03-01", "2026-12-31"),
                                new RateData(15L, 7L, "L4", 160000, "2026-03-01", "2026-12-31"),

                                new RateData(16L, 8L, "L3", 160000, "2026-03-01", "2026-12-31"),
                                new RateData(17L, 8L, "L4", 200000, "2026-03-01", "2026-12-31"),

                                new RateData(18L, 9L, "L2", 90000, "2026-03-01", "2026-12-31"),
                                new RateData(19L, 9L, "L3", 120000, "2026-03-01", "2026-12-31"),
                                new RateData(20L, 9L, "L4", 150000, "2026-03-01", "2026-12-31"),

                                new RateData(21L, 10L, "L2", 100000, "2026-03-01", "2026-12-31"),
                                new RateData(22L, 10L, "L3", 150000, "2026-03-01", "2026-12-31"),

                                new RateData(23L, 11L, "L2", 75000, "2026-03-01", "2026-12-31"),
                                new RateData(24L, 11L, "L3", 110000, "2026-03-01", "2026-12-31"),

                                new RateData(25L, 12L, "L3", 180000, "2026-03-01", "2026-12-31"),

                                new RateData(26L, 13L, "L1", 80000, "2026-03-01", "2026-12-31"),
                                new RateData(27L, 13L, "L2", 110000, "2026-03-01", "2026-12-31"),
                                new RateData(28L, 13L, "L3", 160000, "2026-03-01", "2026-12-31"),

                                new RateData(29L, 14L, "L1", 100000, "2026-03-01", "2026-12-31"),
                                new RateData(30L, 14L, "L2", 160000, "2026-03-01", "2026-12-31"),
                                new RateData(31L, 14L, "L3", 210000, "2026-03-01", "2026-12-31"),
                                new RateData(32L, 14L, "L4", 240000, "2026-03-01", "2026-12-31"),

                                new RateData(33L, 15L, "L1", 100000, "2026-03-01", "2026-12-31"),
                                new RateData(34L, 15L, "L2", 160000, "2026-03-01", "2026-12-31"),
                                new RateData(35L, 15L, "L3", 210000, "2026-03-01", "2026-12-31"),

                                new RateData(36L, 16L, "L4", 220000, "2026-03-01", "2026-12-31"),

                                new RateData(37L, 17L, "L4", 200000, "2026-03-01", "2026-12-31"),
                                new RateData(38L, 17L, "L5", 230000, "2026-03-01", "2026-12-31"),

                                new RateData(39L, 18L, "L3", 180000, "2026-03-01", "2026-12-31"),
                                new RateData(40L, 18L, "L4", 220000, "2026-03-01", "2026-12-31"),

                                new RateData(41L, 19L, "L2", 100000, "2026-03-01", "2026-12-31"),
                                new RateData(42L, 19L, "L3", 150000, "2026-03-01", "2026-12-31"),

                                new RateData(43L, 20L, "L2", 110000, "2026-03-01", "2026-12-31"),
                                new RateData(44L, 20L, "L3", 160000, "2026-03-01", "2026-12-31"),

                                new RateData(45L, 21L, "L2", 75000, "2026-03-01", "2026-12-31"),
                                new RateData(46L, 21L, "L3", 125000, "2026-03-01", "2026-12-31"),
                                new RateData(47L, 21L, "L4", 200000, "2026-03-01", "2026-12-31"),

                                new RateData(48L, 22L, "L1", 90000, "2026-03-01", "2026-12-31"),
                                new RateData(49L, 22L, "L2", 140000, "2026-03-01", "2026-12-31"),
                                new RateData(50L, 22L, "L3", 300000, "2026-03-01", "2026-12-31"),

                                new RateData(51L, 23L, "L3", 200000, "2026-03-01", "2026-12-31"),
                                new RateData(52L, 24L, "L2", 90000, "2026-03-01", "2026-12-31"),
                                new RateData(53L, 24L, "L3", 140000, "2026-03-01", "2026-12-31"),

                                new RateData(54L, 25L, "L2", 45000, "2026-03-01", "2026-12-31"),

                                new RateData(55L, 26L, "L2", 45000, "2026-03-01", "2026-12-31"),
                                new RateData(56L, 27L, "L3", 60000, "2026-03-01", "2026-12-31"),

                                new RateData(57L, 28L, "L2", 120000, "2026-03-01", "2026-12-31"),
                                new RateData(58L, 28L, "L3", 160000, "2026-03-01", "2026-12-31")

                );

                for (RateData item : data) {

                        Integer count = jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM manpower_designation_rate WHERE rate_id = ?",
                                        Integer.class,
                                        item.id());

                        if (count != null && count == 0) {

                                jdbcTemplate.update("""
                                                    INSERT INTO manpower_designation_rate
                                                    (rate_id, designation_id, level_code,
                                                     gross_monthly_ctc, effective_from, effective_to,
                                                     active_flag, created_date_time)
                                                    VALUES (?, ?, ?, ?, ?, ?, 'Y', CURRENT_TIMESTAMP)
                                                """,
                                                item.id(),
                                                item.designationId(),
                                                item.levelCode(),
                                                item.ctc(),
                                                Date.valueOf(item.from()),
                                                Date.valueOf(item.to()));

                                log.info("Bootstrapped rate: id={}, designation={}",
                                                item.id(), item.designationId());
                        }
                }
        }

        private record ResourceLevelData(Long id, String code, String name, double minExp, double maxExp) {
        }

        private record DesignationData(
                        Long id,
                        String category,
                        String name,
                        String qualification,
                        String certification,
                        List<String> levels) {
        }

        private record RateData(Long id, Long designationId, String levelCode, double ctc, String from, String to) {
        }
}
