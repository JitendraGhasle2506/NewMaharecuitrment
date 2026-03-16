package db.postmigration;

import java.sql.Connection;
import java.sql.Date;
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
        }
        if (tableExists(connection, "sub_department")) {
            bootstrapSubDepartments(jdbcTemplate);
        }
        if (tableExists(connection, "resource_level_experience_mst")
                && tableExists(connection, "manpower_designation_master")
                && tableExists(connection, "designation_level_map")
                && tableExists(connection, "manpower_designation_rate")) {
            bootstrapResourceLevels(jdbcTemplate);
            bootstrapDesignations(jdbcTemplate);
            bootstrapDesignationRates(jdbcTemplate);
        }
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

    private void bootstrapDesignations(JdbcTemplate jdbcTemplate) {
        List<DesignationData> data = List.of(
                new DesignationData(1L, "Technical", "Solution Architect - Java",
                        "BTech / BE / MCA (CS / IT / EE)", "AWS Certified Solutions Architect / TOGAF", List.of("L4", "L5")),
                new DesignationData(2L, "Technical", "Solution Architect - .Net",
                        "BTech / BE / MCA (CS / IT / EE)", "Azure Solutions Architect Expert / TOGAF", List.of("L4", "L5")),
                new DesignationData(3L, "Technical", "Quality Manager", "BTech / BE / MCA",
                        "ISO 9001 Lead Auditor / Lean Six Sigma", List.of("L5")),
                new DesignationData(4L, "Technical", "QA Lead", "BTech / BE / MCA", "iSQI CAT", List.of("L4")),
                new DesignationData(5L, "Technical", "Java Developer", "BTech / BE / MCA",
                        "Role based certification", List.of("L2", "L3", "L4")),
                new DesignationData(6L, "Technical", ".Net Developer", "BTech / BE / MCA",
                        "Role based certification", List.of("L2", "L3", "L4")),
                new DesignationData(7L, "Technical", "PHP Developer", "BTech / BE / MCA",
                        "Role based certification", List.of("L2", "L3", "L4")),
                new DesignationData(8L, "Technical", "Full Stack Developer", "BTech / BE / MCA",
                        "MEAN / MERN Certification", List.of("L3", "L4")),
                new DesignationData(9L, "Technical", "DB Developer", "BTech / BE / MCA",
                        "OCA/OCP/MCDBA/Azure DB", List.of("L2", "L3", "L4")),
                new DesignationData(10L, "Technical", "GIS Developer", "BTech / BE GIS / CS",
                        "GIS Certification", List.of("L2", "L3")),
                new DesignationData(11L, "Technical", "Python Developer", "BTech / BE / MCA",
                        "Role based certification", List.of("L2", "L3")),
                new DesignationData(12L, "Technical", "AI & ML Developer", "BTech / BE / MCA",
                        "AI / ML Certification", List.of("L1", "L2", "L3", "L4")),
                new DesignationData(13L, "Technical", "Blockchain Developer", "BTech / BE / MCA",
                        "Blockchain Certification", List.of("L1", "L2", "L3")),
                new DesignationData(14L, "Technical", "Cloud Expert", "BTech / BE / MCA",
                        "AWS / Azure Architect", List.of("L3", "L4")),
                new DesignationData(15L, "Technical", "Mobile App Developer Android", "BTech / BE / MCA",
                        "Google Android Certification", List.of("L2", "L3")),
                new DesignationData(16L, "Technical", "Mobile App Developer iOS", "BTech / BE / MCA",
                        "iOS Certification", List.of("L2", "L3")),
                new DesignationData(17L, "Technical", "AR/VR Developer", "BTech / BE / MCA",
                        "Unity Certification", List.of("L2", "L3", "L4")),
                new DesignationData(18L, "Technical", "Cloud Security Engineer", "BTech / BE / MCA",
                        "CCSP Certification", List.of("L3")),
                new DesignationData(19L, "Technical", "System Analyst", "BTech / BE / MCA",
                        "ITIL Certification", List.of("L2", "L3")),
                new DesignationData(20L, "Techno Functional", "Senior Technical Manager (STM)", "BTech / BE / MCA",
                        "PMP / PRINCE2 / CSM", List.of("L5", "L6", "L7")),
                new DesignationData(21L, "Techno Functional", "Program Lead - IT Solutions & Delivery", "BTech / BE / MCA",
                        "PMP / PRINCE2", List.of("L3", "L4", "L5")),
                new DesignationData(22L, "Techno Functional", "Project Manager", "BTech / BE / MCA", "PMP", List.of("L4")),
                new DesignationData(23L, "Techno Functional", "Project Lead", "BTech / BE / MCA", "", List.of("L3")),
                new DesignationData(24L, "Techno Functional", "Business Analyst", "BTech / BE / MCA", "",
                        List.of("L2", "L3", "L4")),
                new DesignationData(25L, "Techno Functional", "Software Tester", "BTech / BE / MCA", "ISTQB",
                        List.of("L1", "L2", "L3")),
                new DesignationData(26L, "Techno Functional", "Support Engineer", "BTech / BE / MCA", "CompTIA A+",
                        List.of("L1", "L2", "L3")),
                new DesignationData(27L, "Techno Functional", "Network Admin", "BTech / BE / MCA", "CCNA",
                        List.of("L2", "L3")),
                new DesignationData(28L, "Techno Functional", "System Administrator", "BTech / BE / MCA",
                        "Azure Administrator", List.of("L2", "L3")),
                new DesignationData(29L, "Operation & Project Management", "Training Expert", "BTech / BE / MCA",
                        "Training Certification", List.of("L3", "L4")),
                new DesignationData(30L, "Operation & Project Management", "Content Writer / Researcher", "BA / Journalism", "",
                        List.of("L2")),
                new DesignationData(31L, "Operation & Project Management", "Content Writing Manager", "BA / Journalism", "",
                        List.of("L3")),
                new DesignationData(32L, "Operation & Project Management", "Social Media Expert", "Marketing", "",
                        List.of("L1", "L2")),
                new DesignationData(33L, "Operation & Project Management", "PMO / MIS Executive", "BBA / Finance", "",
                        List.of("L2")),
                new DesignationData(34L, "Operation & Project Management", "PMO / MIS Analyst", "BBA / Finance", "",
                        List.of("L3")),
                new DesignationData(35L, "Support", "Helpdesk Support", "Graduate", "ITIL", List.of("L1", "L2")),
                new DesignationData(36L, "Support", "HR / Admin", "MBA HR", "", List.of("L2", "L3", "L4")),
                new DesignationData(37L, "Support", "IT Office Assistant", "Graduate", "MSCIT", List.of("L1", "L2")),
                new DesignationData(38L, "Support", "Data Entry Operator", "Graduate", "MSCIT", List.of("L1", "L2")));

        for (DesignationData item : data) {
            insertIfMissing(
                    jdbcTemplate,
                    "select count(*) from manpower_designation_master where designation_id = ?",
                    "insert into manpower_designation_master "
                            + "(designation_id, category, designation_name, educational_qualification, certification, role_name, active_flag, created_date_time) "
                            + "values (?, ?, ?, ?, ?, '', 'Y', current_timestamp)",
                    item.id(),
                    item.category(),
                    item.name(),
                    item.qualification(),
                    item.certification());

            for (String level : item.levels()) {
                Integer count = jdbcTemplate.queryForObject(
                        "select count(*) from designation_level_map where designation_id = ? and level_code = ?",
                        Integer.class,
                        item.id(),
                        level);
                if (count != null && count == 0) {
                    jdbcTemplate.update(
                            "insert into designation_level_map (designation_id, level_code) values (?, ?)",
                            item.id(),
                            level);
                }
            }
        }
    }

    private void bootstrapDesignationRates(JdbcTemplate jdbcTemplate) {
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
                new RateData(58L, 28L, "L3", 160000, "2026-03-01", "2026-12-31"),
                new RateData(59L, 29L, "L5", 310000, "2026-03-01", "2026-12-31"),
                new RateData(60L, 29L, "L6", 350000, "2026-03-01", "2026-12-31"),
                new RateData(61L, 29L, "L7", 400000, "2026-03-01", "2026-12-31"),
                new RateData(62L, 30L, "L3", 200000, "2026-03-01", "2026-12-31"),
                new RateData(63L, 30L, "L4", 250000, "2026-03-01", "2026-12-31"),
                new RateData(64L, 30L, "L5", 300000, "2026-03-01", "2026-12-31"),
                new RateData(65L, 31L, "L4", 280000, "2026-03-01", "2026-12-31"),
                new RateData(66L, 32L, "L3", 180000, "2026-03-01", "2026-12-31"),
                new RateData(67L, 33L, "L2", 100000, "2026-03-01", "2026-12-31"),
                new RateData(68L, 33L, "L3", 130000, "2026-03-01", "2026-12-31"),
                new RateData(69L, 33L, "L4", 160000, "2026-03-01", "2026-12-31"),
                new RateData(70L, 34L, "L1", 40000, "2026-03-01", "2026-12-31"),
                new RateData(71L, 34L, "L2", 55000, "2026-03-01", "2026-12-31"),
                new RateData(72L, 34L, "L3", 75000, "2026-03-01", "2026-12-31"),
                new RateData(73L, 35L, "L2", 110000, "2026-03-01", "2026-12-31"),
                new RateData(74L, 35L, "L3", 160000, "2026-03-01", "2026-12-31"),
                new RateData(75L, 36L, "L1", 35000, "2026-03-01", "2026-12-31"),
                new RateData(76L, 36L, "L2", 55000, "2026-03-01", "2026-12-31"),
                new RateData(77L, 36L, "L3", 75000, "2026-03-01", "2026-12-31"),
                new RateData(78L, 37L, "L2", 95000, "2026-03-01", "2026-12-31"),
                new RateData(79L, 37L, "L3", 140000, "2026-03-01", "2026-12-31"),
                new RateData(80L, 38L, "L2", 43000, "2026-03-01", "2026-12-31"),
                new RateData(81L, 38L, "L3", 63000, "2026-03-01", "2026-12-31"),
                new RateData(82L, 39L, "L2", 100000, "2026-03-01", "2026-12-31"),
                new RateData(83L, 39L, "L3", 130000, "2026-03-01", "2026-12-31"),
                new RateData(84L, 40L, "L2", 70000, "2026-03-01", "2026-12-31"),
                new RateData(85L, 40L, "L3", 100000, "2026-03-01", "2026-12-31"),
                new RateData(86L, 41L, "L1", 55000, "2026-03-01", "2026-12-31"),
                new RateData(87L, 41L, "L2", 85000, "2026-03-01", "2026-12-31"),
                new RateData(88L, 41L, "L3", 120000, "2026-03-01", "2026-12-31"),
                new RateData(89L, 42L, "L3", 90000, "2026-03-01", "2026-12-31"),
                new RateData(90L, 42L, "L4", 125000, "2026-03-01", "2026-12-31"),
                new RateData(91L, 43L, "L2", 74000, "2026-03-01", "2026-12-31"),
                new RateData(92L, 44L, "L3", 100000, "2026-03-01", "2026-12-31"),
                new RateData(93L, 45L, "L1", 70000, "2026-03-01", "2026-12-31"),
                new RateData(94L, 45L, "L2", 120000, "2026-03-01", "2026-12-31"),
                new RateData(95L, 46L, "L1", 35000, "2026-03-01", "2026-12-31"),
                new RateData(96L, 46L, "L2", 45000, "2026-03-01", "2026-12-31"),
                new RateData(97L, 47L, "L2", 50000, "2026-03-01", "2026-12-31"),
                new RateData(98L, 47L, "L3", 70000, "2026-03-01", "2026-12-31"),
                new RateData(99L, 47L, "L4", 100000, "2026-03-01", "2026-12-31"),
                new RateData(100L, 48L, "L1", 26000, "2026-03-01", "2026-12-31"),
                new RateData(101L, 48L, "L2", 39000, "2026-03-01", "2026-12-31"),
                new RateData(102L, 49L, "L1", 20000, "2026-03-01", "2026-12-31"),
                new RateData(103L, 50L, "L2", 40000, "2026-03-01", "2026-12-31"),
                new RateData(104L, 51L, "L3", 70000, "2026-03-01", "2026-12-31"),
                new RateData(105L, 52L, "L2", 23000, "2026-03-01", "2026-12-31"));

        for (RateData item : data) {
            insertIfMissing(
                    jdbcTemplate,
                    "select count(*) from manpower_designation_rate where rate_id = ?",
                    "insert into manpower_designation_rate "
                            + "(rate_id, designation_id, level_code, gross_monthly_ctc, effective_from, effective_to, active_flag, created_date_time) "
                            + "values (?, ?, ?, ?, ?, ?, 'Y', current_timestamp)",
                    item.id(),
                    item.designationId(),
                    item.levelCode(),
                    item.ctc(),
                    Date.valueOf(item.from()),
                    Date.valueOf(item.to()));
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
