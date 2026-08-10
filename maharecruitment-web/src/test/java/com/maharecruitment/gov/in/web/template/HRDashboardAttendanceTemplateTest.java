package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import com.maharecruitment.gov.in.web.service.dashboard.model.HRCellAttendanceView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRAttendanceDetailCategory;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRAttendanceDetailView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRAttendanceEmployeeView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRAttendanceGrouping;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRAttendanceSummaryView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRTodayAttendanceView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRDepartmentAttendanceView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRDesignationAttendanceView;

class HRDashboardAttendanceTemplateTest {

    private static final Path TEMPLATE_PATH = Path.of(
            "src/main/resources/templates/hr/hr_attendance_today.html");

    private static final Path DASHBOARD_TEMPLATE_PATH = Path.of(
            "src/main/resources/templates/hr/hr_dashboard.html");

    private static final Path ATTENDANCE_STYLES_PATH = Path.of(
            "src/main/resources/static/css/hr-attendance.css");

    private static final Path ATTENDANCE_DETAILS_TEMPLATE_PATH = Path.of(
            "src/main/resources/templates/hr/hr_attendance_details.html");

    @Test
    void dedicatedAttendancePageRendersCheckInAndCellCounts() throws Exception {
        String template = Files.readString(TEMPLATE_PATH);
        int pageStart = template.indexOf("<section class=\"attendance-page\"");
        int pageEnd = template.indexOf("</th:block>", pageStart);
        String page = template.substring(pageStart, pageEnd)
                .replaceAll("\\s+th:href=\"[^\"]+\"", "");

        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        Context context = new Context();
        context.setVariable("attendance", new HRTodayAttendanceView(
                LocalDate.of(2026, 8, 7),
                20,
                11,
                9,
                55,
                new HRAttendanceSummaryView(8, 3, 3, 1, 1),
                HRAttendanceGrouping.CELL,
                List.of(new HRCellAttendanceView(
                        27L,
                        "Network Infra Cell",
                        "MAHAIT Project Cells",
                        12,
                        9,
                        3,
                        75)),
                List.of(),
                List.of()));

        String rendered = engine.process(page, context);

        assertThat(rendered)
                .contains("Early Check-ins")
                .contains("Before 9:45 AM")
                .contains(">3</strong>")
                .contains("Regular Check-ins")
                .contains("9:45 AM to before 10:15 AM")
                .contains(">3</strong>")
                .contains("Late Check-ins")
                .contains("10:15 AM to 11:00 AM")
                .contains(">1</strong>")
                .contains("After 11:00 AM")
                .contains("Cell-wise attendance")
                .contains("Present and Absent")
                .contains("07 Aug 2026")
                .contains("Network Infra Cell")
                .contains("75% present");

        assertThat(template)
                .contains("category='AFTER_ELEVEN'")
                .contains("/hr/attendance-today/details");
    }

    @Test
    void dedicatedAttendancePageUsesClassicResponsivePresentation() throws Exception {
        String template = Files.readString(TEMPLATE_PATH);
        String styles = Files.readString(ATTENDANCE_STYLES_PATH);

        assertThat(template)
                .contains("attendance-page-hero-copy")
                .contains("attendance-date-block")
                .contains("attendance-metric-caption")
                .contains("cell-attendance-controls")
                .contains("aria-live=\"polite\"");

        assertThat(styles)
                .contains("--attendance-navy: #18344d")
                .contains("background: var(--attendance-navy)")
                .contains("font-variant-numeric: tabular-nums")
                .contains("@media (max-width: 767.98px)")
                .contains("@media (prefers-reduced-motion: reduce)");
    }

    @Test
    void attendanceCountDetailsRenderEmployeeNameAndCheckInTime() throws Exception {
        String template = Files.readString(ATTENDANCE_DETAILS_TEMPLATE_PATH);
        int pageStart = template.indexOf("<section class=\"attendance-page attendance-detail-page\"");
        int pageEnd = template.indexOf("</th:block>", pageStart);
        String page = template.substring(pageStart, pageEnd)
                .replaceAll("\\s+th:href=\"[^\"]+\"", "");

        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        Context context = new Context();
        context.setVariable("attendanceDetail", new HRAttendanceDetailView(
                LocalDate.of(2026, 8, 7),
                HRAttendanceDetailCategory.LATE,
                27L,
                "Network Infra Cell",
                null,
                null,
                null,
                null,
                List.of(new HRAttendanceEmployeeView(
                        101L,
                        "EMP-101",
                        "Asha Employee",
                        "INTERNAL",
                        "PRESENT",
                        java.time.LocalTime.of(10, 32))),
                0,
                25,
                false,
                false));

        String rendered = engine.process(page, context);

        assertThat(rendered)
                .contains("Late Check-ins")
                .contains("Network Infra Cell")
                .contains("07 Aug 2026")
                .contains("Asha Employee")
                .contains("EMP-101")
                .contains("10:32 am")
                .doesNotContain("Wing");
    }

    @Test
    void dedicatedAttendancePageRendersDesignationWisePresentAttendance() throws Exception {
        String template = Files.readString(TEMPLATE_PATH);
        int pageStart = template.indexOf("<section class=\"attendance-page\"");
        int pageEnd = template.indexOf("</th:block>", pageStart);
        String page = template.substring(pageStart, pageEnd)
                .replaceAll("\\s+th:href=\"[^\"]+\"", "");

        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        Context context = new Context();
        context.setVariable("attendance", new HRTodayAttendanceView(
                LocalDate.of(2026, 8, 7),
                10,
                6,
                4,
                60,
                new HRAttendanceSummaryView(5, 1, 2, 1, 1),
                HRAttendanceGrouping.DESIGNATION,
                List.of(),
                List.of(new HRDesignationAttendanceView(
                        9L,
                        "Software Developer",
                        8,
                        6,
                        2,
                        75)),
                List.of()));

        String rendered = engine.process(page, context);

        assertThat(rendered)
                .contains("View attendance by")
                .contains("Designation-wise")
                .contains("Designation-wise attendance")
                .contains("Software Developer")
                .contains("75% present")
                .doesNotContain("No active designations");
        assertThat(template)
                .contains("groupBy='DESIGNATION'")
                .contains("designationId=${designation.designationId}");
    }

    @Test
    void dedicatedAttendancePageRendersExternalDepartmentAttendance() throws Exception {
        String template = Files.readString(TEMPLATE_PATH);
        int pageStart = template.indexOf("<section class=\"attendance-page\"");
        int pageEnd = template.indexOf("</th:block>", pageStart);
        String page = template.substring(pageStart, pageEnd)
                .replaceAll("\\s+th:href=\"[^\"]+\"", "");

        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        Context context = new Context();
        context.setVariable("attendance", new HRTodayAttendanceView(
                LocalDate.of(2026, 8, 7),
                10,
                6,
                4,
                60,
                new HRAttendanceSummaryView(5, 1, 2, 1, 1),
                HRAttendanceGrouping.DEPARTMENT,
                List.of(),
                List.of(),
                List.of(new HRDepartmentAttendanceView(
                        4L,
                        "Finance Department",
                        8,
                        5,
                        3,
                        62))));

        String rendered = engine.process(page, context);

        assertThat(rendered)
                .contains("Department-wise")
                .contains("Department-wise attendance")
                .contains("External Employee Attendance")
                .contains("external employees only")
                .contains("Finance Department")
                .contains("62% present");
        assertThat(template)
                .contains("groupBy='DEPARTMENT'")
                .contains("departmentId=${department.departmentId}");
    }

    @Test
    void dashboardLinksToDedicatedAttendancePageWithoutEmbeddingDetails() throws Exception {
        String dashboardTemplate = Files.readString(DASHBOARD_TEMPLATE_PATH);

        assertThat(dashboardTemplate)
                .contains("th:href=\"@{/hr/attendance-today}\"")
                .doesNotContain("id=\"hrAttendanceDetails\"")
                .doesNotContain("data-attendance-toggle")
                .doesNotContain("setAttendanceDetailsVisible");
    }
}
