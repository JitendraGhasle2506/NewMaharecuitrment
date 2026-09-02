package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import com.maharecruitment.gov.in.attendance.dto.AttendanceDayDTO;
import com.maharecruitment.gov.in.attendance.dto.AttendanceRegisterDTO;
import com.maharecruitment.gov.in.attendance.service.model.TeamAttendanceMemberView;
import com.maharecruitment.gov.in.attendance.service.model.TeamAttendanceOverview;

class TeamAttendanceTemplateTest {

    private static final Path OVERVIEW_TEMPLATE = Path.of(
            "src/main/resources/templates/attendance/team-attendance-list.html");
    private static final Path DETAIL_TEMPLATE = Path.of(
            "src/main/resources/templates/attendance/view-employee-attendance.html");

    @Test
    void overviewRendersTeamMetricsAndEmployeeAttendance() throws Exception {
        TeamAttendanceMemberView member = member();
        TeamAttendanceOverview overview = new TeamAttendanceOverview(
                YearMonth.of(2026, 8),
                LocalDate.of(2026, 8, 31),
                List.of(member),
                18,
                1,
                1,
                0,
                90);
        Context context = baseContext();
        context.setVariable("overview", overview);
        context.setVariable("teamMembers", overview.members());

        String rendered = renderSection(OVERVIEW_TEMPLATE, "<section class=\"team-attendance-shell\"", context);

        assertThat(rendered)
                .contains("Attendance under your authority")
                .contains("Team attendance overview")
                .contains("Asha Patil")
                .contains("EMP101")
                .contains("Recruitment Portal")
                .contains("status-present")
                .contains("90%");
    }

    @Test
    void detailRendersMonthlyTilesAndDailyPunchLog() throws Exception {
        AttendanceDayDTO day = new AttendanceDayDTO();
        day.setDate(LocalDate.of(2026, 8, 3));
        day.setStatus("PRESENT");
        day.setInTime("09:30");
        day.setOutTime("18:00");
        day.setStayHours("08:30");
        AttendanceRegisterDTO attendance = new AttendanceRegisterDTO();
        attendance.setOrganization("MahaIT");
        attendance.setOfficeLocation("Mumbai");
        attendance.setTotalPresent(1);
        attendance.setAttendanceDays(List.of(day));
        Context context = baseContext();
        context.setVariable("member", member());
        context.setVariable("attendance", attendance);

        String rendered = renderSection(DETAIL_TEMPLATE, "<section class=\"team-attendance-shell attendance-detail-shell\"", context);

        assertThat(rendered)
                .contains("Employee attendance register")
                .contains("Monthly status")
                .contains("Daily attendance log")
                .contains("03 Aug 2026")
                .contains("09:30")
                .contains("18:00")
                .contains("status-present");
    }

    private Context baseContext() {
        Context context = new Context();
        context.setVariable("selectedMonth", 8);
        context.setVariable("selectedYear", 2026);
        context.setVariable("periodLabel", "August 2026");
        context.setVariable("monthNames", Map.of(8, "August"));
        context.setVariable("yearsList", List.of(2026));
        return context;
    }

    private TeamAttendanceMemberView member() {
        return new TeamAttendanceMemberView(
                101L,
                "EMP101",
                "Asha Patil",
                "AP",
                "Developer",
                "Applications",
                "Recruitment Portal",
                "PRESENT",
                "09:30",
                "18:00",
                18,
                1,
                1,
                0,
                1,
                8,
                90);
    }

    private String renderSection(Path path, String sectionStart, Context context) throws Exception {
        String template = Files.readString(path);
        int start = template.indexOf(sectionStart);
        int end = template.indexOf("</th:block>", start);
        String section = template.substring(start, end)
                .replaceAll("\\s+th:(href|action)=\"[^\"]+\"", "");

        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine.process(section, context);
    }
}
