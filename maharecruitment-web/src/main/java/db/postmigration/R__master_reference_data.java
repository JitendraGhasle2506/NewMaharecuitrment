package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class R__master_reference_data extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));

        if (tableExists(connection, "department_mst")) {
            bootstrapDepartments(jdbcTemplate);
            syncIdentitySequence(jdbcTemplate, "department_mst", "department_id");
        }
        if (tableExists(connection, "sub_department")) {
            bootstrapSubDepartments(jdbcTemplate);
            syncIdentitySequence(jdbcTemplate, "sub_department", "sub_dept_id");
        }
        if (tableExists(connection, "resource_level_experience_mst")) {
            bootstrapResourceLevels(jdbcTemplate);
        }
    }

    private void syncIdentitySequence(JdbcTemplate jdbcTemplate, String tableName, String idColumn) {
        String sequenceName = jdbcTemplate.queryForObject(
                "select pg_get_serial_sequence(?, ?)",
                String.class,
                tableName,
                idColumn);

        if (sequenceName == null || sequenceName.isBlank()) {
            return;
        }

        Long nextValue = jdbcTemplate.queryForObject(
                "select coalesce(max(" + idColumn + "), 0) + 1 from " + tableName,
                Long.class);

        jdbcTemplate.queryForObject(
                "select setval(cast(? as regclass), ?, false)",
                Long.class,
                sequenceName,
                nextValue);
    }

    private void bootstrapDepartments(JdbcTemplate jdbcTemplate) {
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

        departments.forEach((id, name) -> insertIfMissing(
                jdbcTemplate,
                "select count(*) from department_mst where department_id = ?",
                "insert into department_mst (department_id, department_name, created_date_time) values (?, ?, current_timestamp)",
                id,
                name));
    }

    private void bootstrapSubDepartments(JdbcTemplate jdbcTemplate) {
        List<SubDeptData> data = List.of(
                new SubDeptData(1L, "Animal Husbandry", 17L),
                new SubDeptData(2L, "Fisheries", 17L),
                new SubDeptData(3L, "Cooperative", 2L),
                new SubDeptData(6L, "Directorate of Accounts & Treasuries", 3L),
                new SubDeptData(7L, "Directorate of Insurance", 3L),
                new SubDeptData(8L, "Directorate of Local Fund Accounts Audit", 3L),
                new SubDeptData(9L, "GST", 3L),
                new SubDeptData(10L, "Forest Department", 5L),
                new SubDeptData(11L, "Directorate Of Art", 6L),
                new SubDeptData(12L, "Directorate of Higher Education", 6L),
                new SubDeptData(13L, "Directorate of Libraries", 6L),
                new SubDeptData(14L, "The Directorate of Technical Education", 6L),
                new SubDeptData(15L, "Directorate of Economics and Statistics", 15L),
                new SubDeptData(16L, "Controller of Legal Metrology", 4L),
                new SubDeptData(17L, "Controller of Rationing and Director Civil Supplies", 4L),
                new SubDeptData(18L, "Financial Adviser and Deputy Secretary", 4L),
                new SubDeptData(19L, "Office of the Commissioner of Supply", 4L),
                new SubDeptData(20L, "State Consumer Disputes Redressal Commission", 4L),
                new SubDeptData(21L, "Directorate of Government Printing, Stationery and Publication", 7L),
                new SubDeptData(22L, "Directorate of Industrial Safety and Health", 7L),
                new SubDeptData(23L, "Directorate of Industries", 7L),
                new SubDeptData(24L, "Directorate of Boiler", 7L),
                new SubDeptData(25L, "Energy", 7L),
                new SubDeptData(26L, "Labour Commissioner", 7L),
                new SubDeptData(27L, "Directorate of Languages, Mumbai", 13L),
                new SubDeptData(28L, "Maharashtra Rajya Marathi Vishwakosh Nirmiti Mandala", 13L),
                new SubDeptData(29L, "Maharashtra State Board for Literature and Culture, Mumbai", 13L),
                new SubDeptData(30L, "Maharashtra Development Services", 11L),
                new SubDeptData(32L, "Commissionerate of Social Welfare", 8L),
                new SubDeptData(34L, "Sericulture Directorate", 2L),
                new SubDeptData(35L, "Textile Directorate", 2L),
                new SubDeptData(36L, "Chief Officer", 14L),
                new SubDeptData(37L, "Directorate of Maharashtra Fire & Emergency Services", 14L),
                new SubDeptData(38L, "Directorate of Municipal Administration", 14L),
                new SubDeptData(39L, "Commissioner, Women and Child Development", 12L),
                new SubDeptData(42L, "Office of Charity Commissioner, Maharashtra State, Mumbai", 21L),
                new SubDeptData(43L, "Office of Registrar of Partnership Firm", 21L),
                new SubDeptData(44L, "Office of the Administrator General and Official Trustee", 21L),
                new SubDeptData(45L, "Law and Judiciary Department", 21L),
                new SubDeptData(46L, "Government Pleader Offices Thirteen", 21L),
                new SubDeptData(47L, "Ground Water Survey and Development Agency", 22L),
                new SubDeptData(48L, "Tribal Development Department", 23L),
                new SubDeptData(50L, "Directorate of Other Backward Bahujan Welfare, Pune", 25L),
                new SubDeptData(51L, "Directorate of Vocational Education and Training", 27L),
                new SubDeptData(52L, "Skill Development, Employment and Entrepreneurship Commissionerate", 27L),
                new SubDeptData(53L, "Directorate of Tourism", 28L),
                new SubDeptData(54L, "Directorate of Geology and Mining, Nagpur", 7L),
                new SubDeptData(55L, "Revenue", 30L),
                new SubDeptData(56L, "Land Record", 30L),
                new SubDeptData(57L, "Registration and Stamps", 30L),
                new SubDeptData(58L, "Water Resources", 31L),
                new SubDeptData(59L, "Medical Education and Research, Mumbai", 32L),
                new SubDeptData(60L, "Stage Performances Scrutiny Board", 33L),
                new SubDeptData(61L, "P. L. Deshpande Maharashtra Kala Academy", 33L),
                new SubDeptData(62L, "Gazetteer Department", 33L),
                new SubDeptData(63L, "Directorate of Archives", 33L),
                new SubDeptData(64L, "Directorate of Archaeology & Museums", 33L),
                new SubDeptData(65L, "Directorate of Cultural Affairs", 33L),
                new SubDeptData(66L, "State Language Literature Academies", 33L),
                new SubDeptData(68L, "Director General of Police", 19L),
                new SubDeptData(69L, "Transport", 19L),
                new SubDeptData(70L, "Directorate of Prosecution", 19L),
                new SubDeptData(71L, "Directorate of Forensic Science", 19L),
                new SubDeptData(72L, "Commissioner State Excise", 19L),
                new SubDeptData(73L, "Prisons and Correctional Services", 19L),
                new SubDeptData(74L, "Directorate of Information and Public Relations", 18L),
                new SubDeptData(75L, "Commissionerate for Persons with Disabilities Welfare", 34L),
                new SubDeptData(76L, "Agriculture", 17L),
                new SubDeptData(77L, "Public Works Department (Civil)", 20L),
                new SubDeptData(78L, "Directors Chawl Development Department, Mumbai", 20L),
                new SubDeptData(79L, "Chief Architect to Government", 20L),
                new SubDeptData(80L, "Superintending Engineer Electrical Establishment", 20L),
                new SubDeptData(81L, "Director, Parks and Gardens", 20L),
                new SubDeptData(82L, "Superintending Engineer (Mechanical Circle), New Mumbai", 20L),
                new SubDeptData(83L, "Administration-4, PWD Mantralaya", 20L),
                new SubDeptData(84L, "Food & Drugs Department", 32L),
                new SubDeptData(85L, "Mantralaya Canteen", 18L),
                new SubDeptData(86L, "Government Transport Services", 18L),
                new SubDeptData(88L, "Department of Sainik Welfare, Pune", 18L),
                new SubDeptData(91L, "Town Planning and Valuation Directorate", 14L),
                new SubDeptData(92L, "Mantralaya Proper", 18L),
                new SubDeptData(93L, "Directorate of Ayush", 32L),
                new SubDeptData(100L, "Directorate of Sports and Youth Services", 35L),
                new SubDeptData(101L, "National Cadet Corps", 35L),
                new SubDeptData(102L, "Public Health Commissionerate", 26L),
                new SubDeptData(103L, "Directorate of Civil Defence", 19L),
                new SubDeptData(104L, "Commandant General, Home Guards", 19L),
                new SubDeptData(105L, "Sahyadri and Nandgiri Guest House", 18L),
                new SubDeptData(106L, "Office of Comptroller of Household to the Governor", 18L),
                new SubDeptData(107L, "Office of Resident Commissioner, Maharashtra Sadan", 18L),
                new SubDeptData(108L, "Office of Secretary to the Governor, Lok Bhavan", 18L),
                new SubDeptData(109L, "Service Preparatory Institute", 18L),
                new SubDeptData(110L, "School Education Commissionerate", 24L),
                new SubDeptData(111L, "Employees State Insurance Scheme", 26L),
                new SubDeptData(112L, "Minority Commissionerate & Minorities Cell", 36L),
                new SubDeptData(113L, "Maharashtra State Waqf Tribunal", 36L),
                new SubDeptData(114L, "Maharashtra State Urdu Sahitya Academy", 36L));

        for (SubDeptData item : data) {
            insertIfMissing(
                    jdbcTemplate,
                    "select count(*) from sub_department where sub_dept_id = ?",
                    "insert into sub_department (sub_dept_id, sub_dept_name, department_id, created_date_time) values (?, ?, ?, current_timestamp)",
                    item.id(),
                    item.name(),
                    item.departmentId());
        }
    }

    private void bootstrapResourceLevels(JdbcTemplate jdbcTemplate) {
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
            insertIfMissing(
                    jdbcTemplate,
                    "select count(*) from resource_level_experience_mst where level_id = ?",
                    "insert into resource_level_experience_mst (level_id, level_code, level_name, min_experience, max_experience, active_flag, created_date_time) "
                            + "values (?, ?, ?, ?, ?, 'Y', current_timestamp)",
                    item.id(),
                    item.code(),
                    item.name(),
                    item.minExperience(),
                    item.maxExperience());
        }
    }

    private void insertIfMissing(JdbcTemplate jdbcTemplate, String countSql, String insertSql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(countSql, Integer.class, args[0]);
        if (count != null && count == 0) {
            jdbcTemplate.update(insertSql, args);
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }

    private record SubDeptData(Long id, String name, Long departmentId) {
    }

    private record ResourceLevelData(Long id, String code, String name, double minExperience, double maxExperience) {
    }
}
