package com.maharecruitment.gov.in.attendance.controller;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.maharecruitment.gov.in.attendance.dto.AttendanceRegisterDTO;
import com.maharecruitment.gov.in.attendance.service.AttendanceRegisterService;
import com.maharecruitment.gov.in.attendance.service.TeamAttendanceService;
import com.maharecruitment.gov.in.attendance.service.model.TeamAttendanceMemberView;
import com.maharecruitment.gov.in.attendance.service.model.TeamAttendanceOverview;
import com.maharecruitment.gov.in.common.dto.SessionUserDTO;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping({ "/reporting-manager/attendance", "/hod1/team-attendance" })
public class TeamAttendanceController {

    private static final int EARLIEST_ATTENDANCE_YEAR = 2020;
    private static final DateTimeFormatter PERIOD_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    private final AttendanceRegisterService attendanceService;
    private final TeamAttendanceService teamAttendanceService;

    public TeamAttendanceController(
            AttendanceRegisterService attendanceService,
            TeamAttendanceService teamAttendanceService) {
        this.attendanceService = attendanceService;
        this.teamAttendanceService = teamAttendanceService;
    }

    @GetMapping
    public String listTeam(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Model model,
            HttpSession session) {
        SessionUserDTO sessionUser = requireSessionUser(session);
        YearMonth period = resolvePeriod(month, year);
        TeamAttendanceOverview overview = teamAttendanceService.getOverview(sessionUser.id(), period);

        model.addAttribute("overview", overview);
        model.addAttribute("teamMembers", overview.members());
        populatePeriodModel(model, period);
        return "attendance/team-attendance-list";
    }

    @GetMapping("/view")
    public String viewMemberAttendance(
            @RequestParam("empId") Long employeeId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Model model,
            HttpSession session) {
        SessionUserDTO sessionUser = requireSessionUser(session);
        YearMonth period = resolvePeriod(month, year);
        TeamAttendanceMemberView member = teamAttendanceService
                .getAuthorizedMember(sessionUser.id(), employeeId, period)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "This employee is outside your reporting authority."));

        AttendanceRegisterDTO attendance = attendanceService.getInternalAttendanceForEmployee(
                member.employeeId(),
                period.getMonthValue(),
                period.getYear());
        attendance.setDateRange("%02d-%d".formatted(period.getMonthValue(), period.getYear()));

        model.addAttribute("attendance", attendance);
        model.addAttribute("member", member);
        model.addAttribute("daysInMonth", period.lengthOfMonth());
        populatePeriodModel(model, period);
        return "attendance/view-employee-attendance";
    }

    private SessionUserDTO requireSessionUser(HttpSession session) {
        SessionUserDTO sessionUser = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (sessionUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Your session has expired.");
        }
        return sessionUser;
    }

    private YearMonth resolvePeriod(Integer month, Integer year) {
        YearMonth currentPeriod = YearMonth.now();
        if (month == null || year == null) {
            return currentPeriod;
        }
        try {
            YearMonth requestedPeriod = YearMonth.of(year, month);
            if (requestedPeriod.getYear() < EARLIEST_ATTENDANCE_YEAR) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attendance year.");
            }
            return requestedPeriod.isAfter(currentPeriod) ? currentPeriod : requestedPeriod;
        } catch (DateTimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attendance month.", exception);
        }
    }

    private void populatePeriodModel(Model model, YearMonth period) {
        LocalDate today = LocalDate.now();
        model.addAttribute("selectedMonth", period.getMonthValue());
        model.addAttribute("selectedYear", period.getYear());
        model.addAttribute("periodLabel", PERIOD_FORMATTER.format(period));
        model.addAttribute("monthNames", monthNames());
        int firstYear = Math.min(period.getYear(), today.getYear() - 4);
        model.addAttribute("yearsList", IntStream.rangeClosed(firstYear, today.getYear())
                .boxed()
                .sorted((left, right) -> Integer.compare(right, left))
                .toList());
    }

    private Map<Integer, String> monthNames() {
        Map<Integer, String> months = new TreeMap<>();
        for (int month = 1; month <= 12; month++) {
            months.put(month, java.time.Month.of(month).getDisplayName(
                    java.time.format.TextStyle.FULL,
                    Locale.ENGLISH));
        }
        return months;
    }
}
