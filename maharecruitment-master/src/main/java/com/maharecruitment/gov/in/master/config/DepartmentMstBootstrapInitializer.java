package com.maharecruitment.gov.in.master.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class DepartmentMstBootstrapInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Starting Department Master data bootstrap using JdbcTemplate...");

        Map<Long, String> departments = Map.ofEntries(
                Map.entry(2L, "Cooperation, Marketing and Textiles Department"),
                Map.entry(3L, "Finance Department"),
                Map.entry(4L, "Food Civil Supply and Consumer Protection Department"),
                Map.entry(5L, "Forest Department"),
                Map.entry(6L, "Higher and Technical Education Department"),
                Map.entry(7L, "Industry, Energy, Labour and Mining Department"),
                Map.entry(8L, "Social Justice and Special Assistance Department"),
                Map.entry(10L, "Textile Department"),
                Map.entry(11L, "Rural Development Department"),
                Map.entry(12L, "Women and Child Development Department"),
                Map.entry(13L, "Marathi Language Department"),
                Map.entry(14L, "Urban Development Department"),
                Map.entry(15L, "Planning Department"),
                Map.entry(16L, "Soil and Water Conservation Department"),
                Map.entry(17L, "Agriculture, Dairy Development, Animal Husbandry and Fisheries Department"),
                Map.entry(18L, "General Administration Department"),
                Map.entry(19L, "Home Department"),
                Map.entry(20L, "Public Work Department"),
                Map.entry(21L, "Law and Judiciary Department"),
                Map.entry(22L, "Water Supply and Sanitation Department"),
                Map.entry(23L, "Tribal Development Department"),
                Map.entry(24L, "School Education Department"),
                Map.entry(25L, "Other Backward Bahujan Welfare Department"),
                Map.entry(26L, "Public Health Department"),
                Map.entry(27L, "Skill Development Department"),
                Map.entry(28L, "Tourism Department"),
                Map.entry(30L, "Revenue Department"),
                Map.entry(31L, "Water Resource Department"),
                Map.entry(32L, "Medical Education and Drugs Department"),
                Map.entry(33L, "Cultural Affairs Department"),
                Map.entry(34L, "Persons with Disabilities Welfare Department"),
                Map.entry(35L, "Sports Department"),
                Map.entry(36L, "Minority Development Department"));

        departments.forEach((id, name) -> {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM department_mst WHERE department_id = ?",
                    Integer.class, id);

            if (count != null && count == 0) {
                jdbcTemplate.update(
                        "INSERT INTO department_mst (department_id, department_name, created_date_time) VALUES (?, ?, CURRENT_TIMESTAMP)",
                        id, name);
                log.info("Bootstrapped department: {} - {}", id, name);
            }
        });

        log.info("Department Master data bootstrap completed.");
    }
}
