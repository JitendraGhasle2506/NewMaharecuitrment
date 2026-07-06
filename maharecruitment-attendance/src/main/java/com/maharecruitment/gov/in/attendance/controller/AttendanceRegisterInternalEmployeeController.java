package com.maharecruitment.gov.in.attendance.controller;

import java.text.DateFormatSymbols;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.maharecruitment.gov.in.attendance.dto.AttendanceCalendarDayDTO;
import com.maharecruitment.gov.in.attendance.dto.AttendanceDayDTO;
import com.maharecruitment.gov.in.attendance.dto.AttendanceRegisterDTO;
import com.maharecruitment.gov.in.attendance.dto.ManualAttendanceRequestDTO;
import com.maharecruitment.gov.in.attendance.service.AttendanceRegisterService;
import com.maharecruitment.gov.in.attendance.service.HolidayService;
import com.maharecruitment.gov.in.attendance.service.WeekOffWorkingDayService;
import com.maharecruitment.gov.in.common.dto.SessionUserDTO;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/employee")
public class AttendanceRegisterInternalEmployeeController {

    @Autowired
    private AttendanceRegisterService attendanceService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private HolidayService holidayService;

    @Autowired
    private WeekOffWorkingDayService weekOffWorkingDayService;

    @GetMapping("/intAttendance")
    public String myAttendance(Model model, HttpSession session) {
        EmployeeEntity employee = resolveCurrentEmployee(session);

        Long employeeId = employee.getEmployeeId();
        if (employeeId == null) {
            model.addAttribute("error", "Employee mapping not found in user account.");
            return "attendance/attendance-register-internal";
        }

        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year = today.getYear();
        populateAttendanceView(model, employee, month, year, today);

        return "attendance/attendance-register-internal";
    }

    @GetMapping("/attendance-calendar")
    public String attendanceCalendar(
            @RequestParam(required = false) String dateRange,
            Model model,
            HttpSession session) {

        EmployeeEntity employee = resolveCurrentEmployee(session);
        if (employee.getEmployeeId() == null) {
            model.addAttribute("error", "Employee mapping not found in user account.");
            return "attendance/employee-attendance-calendar";
        }

        LocalDate today = LocalDate.now();
        YearMonth selectedMonth = resolveSelectedMonth(dateRange, today);
        populateAttendanceCalendarView(model, employee, selectedMonth, today);
        return "attendance/employee-attendance-calendar";
    }

    @PostMapping("/fetchMyAttendance")
    public String fetchMyAttendance(@RequestParam(required = false) String dateRange,
            Model model, HttpSession session) {

        EmployeeEntity employee = resolveCurrentEmployee(session);

        Long employeeId = employee.getEmployeeId();
        if (employeeId == null) {
            model.addAttribute("error", "Employee mapping not found in user account.");
            return "attendance/attendance-register-internal";
        }

        LocalDate today = LocalDate.now();
        YearMonth selectedMonth = resolveSelectedMonth(dateRange, today);
        populateAttendanceView(model, employee, selectedMonth.getMonthValue(), selectedMonth.getYear(), today);

        return "attendance/attendance-register-internal";
    }

    private void populateAttendanceView(
            Model model,
            EmployeeEntity employee,
            int month,
            int year,
            LocalDate today) {
        AttendanceRegisterDTO attendance = attendanceService.getInternalAttendanceForEmployee(employee.getEmployeeId(), month, year);
        attendance.setDateRange(String.format("%02d-%d", month, year));

        model.addAttribute("attendance", attendance);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedYear", year);
        model.addAttribute("monthNames", getMonthNames());
        model.addAttribute("today", today);
        model.addAttribute("attendanceTimeRows", extractAttendanceTimeRows(attendance));

        YearMonth yearMonth = YearMonth.of(year, month);
        model.addAttribute("daysInMonth", yearMonth.lengthOfMonth());

        List<ManualAttendanceRequestDTO> allRequests = attendanceService.getMyManualRequests(employee.getEmployeeId());
        final int fMonth = month;
        final int fYear = year;
        model.addAttribute("pendingRequests", allRequests.stream()
            .filter(r -> r.getAttendanceDate().getMonthValue() == fMonth && r.getAttendanceDate().getYear() == fYear)
            .filter(r -> "PENDING".equalsIgnoreCase(r.getHodStatus()))
            .collect(Collectors.toList()));
        model.addAttribute("approvedRequests", allRequests.stream()
            .filter(r -> r.getAttendanceDate().getMonthValue() == fMonth && r.getAttendanceDate().getYear() == fYear)
            .filter(r -> "APPROVED".equalsIgnoreCase(r.getHodStatus()))
            .collect(Collectors.toList()));
        model.addAttribute("rejectedRequests", allRequests.stream()
            .filter(r -> r.getAttendanceDate().getMonthValue() == fMonth && r.getAttendanceDate().getYear() == fYear)
            .filter(r -> "REJECTED".equalsIgnoreCase(r.getHodStatus()))
            .collect(Collectors.toList()));
    }

    private void populateAttendanceCalendarView(
            Model model,
            EmployeeEntity employee,
            YearMonth selectedMonth,
            LocalDate today) {
        AttendanceRegisterDTO attendance = attendanceService.getInternalAttendanceForEmployee(
                employee.getEmployeeId(),
                selectedMonth.getMonthValue(),
                selectedMonth.getYear());
        attendance.setDateRange(String.format("%02d-%d", selectedMonth.getMonthValue(), selectedMonth.getYear()));

        model.addAttribute("attendance", attendance);
        model.addAttribute("selectedMonth", selectedMonth.getMonthValue());
        model.addAttribute("selectedYear", selectedMonth.getYear());
        model.addAttribute("monthNames", getMonthNames());
        model.addAttribute("today", today);
        model.addAttribute("daysInMonth", selectedMonth.lengthOfMonth());
        model.addAttribute(
                "attendanceMonthLabel",
                selectedMonth.getMonth().getDisplayName(TextStyle.FULL, java.util.Locale.ENGLISH)
                        + " "
                        + selectedMonth.getYear());
        model.addAttribute("attendanceCalendarWeeks", buildCalendarWeeks(selectedMonth, today));
    }

    private List<AttendanceDayDTO> extractAttendanceTimeRows(AttendanceRegisterDTO attendance) {
        if (attendance == null || attendance.getAttendanceDays() == null) {
            return List.of();
        }

        return attendance.getAttendanceDays().stream()
                .filter(day -> day != null && (StringUtils.hasText(day.getInTime()) || StringUtils.hasText(day.getOutTime())))
                .collect(Collectors.toList());
    }

    private Map<Integer, String> getMonthNames() {
        Map<Integer, String> monthMap = new TreeMap<>();
        String[] months = new DateFormatSymbols().getMonths();
        for (int i = 0; i < 12; i++) {
            monthMap.put(i + 1, months[i]);
        }
        return monthMap;
    }

    private List<List<AttendanceCalendarDayDTO>> buildCalendarWeeks(YearMonth selectedMonth, LocalDate today) {
        LocalDate monthStart = selectedMonth.atDay(1);
        LocalDate monthEnd = selectedMonth.atEndOfMonth();
        Map<LocalDate, String> holidayRemarksByDate = holidayService.getHolidaysBetween(monthStart, monthEnd).stream()
                .collect(Collectors.toMap(
                        holiday -> holiday.getHolidayDate(),
                        holiday -> holiday.getHolidayName(),
                        (first, second) -> first));
        Set<LocalDate> holidayDates = holidayRemarksByDate.keySet();
        Set<LocalDate> workingDayOverrideDates = weekOffWorkingDayService.getWorkingDayDatesBetween(monthStart, monthEnd);

        List<List<AttendanceCalendarDayDTO>> weeks = new ArrayList<>();
        LocalDate cursor = monthStart;
        while (cursor.getDayOfWeek() != DayOfWeek.MONDAY) {
            cursor = cursor.minusDays(1);
        }

        LocalDate gridEnd = monthEnd;
        while (gridEnd.getDayOfWeek() != DayOfWeek.SUNDAY) {
            gridEnd = gridEnd.plusDays(1);
        }

        while (!cursor.isAfter(gridEnd)) {
            List<AttendanceCalendarDayDTO> week = new ArrayList<>();
            for (int index = 0; index < 7; index++) {
                boolean holiday = holidayDates.contains(cursor);
                boolean weekOff = isWeekend(cursor) && !workingDayOverrideDates.contains(cursor) && !holiday;
                boolean workingDay = !holiday && !weekOff;
                String holidayRemark = holiday ? holidayRemarksByDate.get(cursor) : null;
                week.add(new AttendanceCalendarDayDTO(
                        cursor,
                        YearMonth.from(cursor).equals(selectedMonth),
                        cursor.equals(today),
                        holiday,
                        weekOff,
                        workingDay,
                        holidayRemark));
                cursor = cursor.plusDays(1);
            }
            weeks.add(week);
        }

        return weeks;
    }

    private EmployeeEntity resolveCurrentEmployee(HttpSession session) {
        SessionUserDTO sessionUser = (SessionUserDTO) session.getAttribute("SESSION_USER");
        return employeeRepository.findByEmail(sessionUser.email())
                .orElseThrow(() -> new IllegalArgumentException("Employee record not found"));
    }

    private YearMonth resolveSelectedMonth(String dateRange, LocalDate today) {
        int month = today.getMonthValue();
        int year = today.getYear();

        if (dateRange != null && !dateRange.isEmpty()) {
            try {
                String[] parts = dateRange.split("-");
                if (parts.length == 2) {
                    month = Integer.parseInt(parts[0]);
                    year = Integer.parseInt(parts[1]);
                }
            } catch (Exception e) {
                // Fallback to current month/year on parse error
            }
        }

        return YearMonth.of(year, month);
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    @PostMapping("/manual-attendance/submit-bulk")
    public String submitBulkManualAttendance(
            @ModelAttribute AttendanceRegisterDTO dto,
            HttpSession session, 
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttrs) {
        
        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        EmployeeEntity employee = employeeRepository.findByEmail(user.email())
                .orElseThrow(() -> new IllegalArgumentException("Employee record not found"));

        
        try {
            int submittedCount = 0;
            int skippedCount = 0;
            if (dto.getAttendanceDays() != null) {
                for (com.maharecruitment.gov.in.attendance.dto.AttendanceDayDTO day : dto.getAttendanceDays()) {
                    if ("PRESENT".equalsIgnoreCase(day.getStatus())) {
                        String autoInTime = "10:00";
                        String autoOutTime = "18:00";
                        String reason = "Manual Attendance Mark from Dashboard Grid";
                        
                        if (attendanceService.submitManualAttendance(employee.getEmployeeId(), day.getDate(), autoInTime, autoOutTime, reason)) {
                            submittedCount++;
                        } else {
                            skippedCount++;
                        }
                    }
                }
            }
            if(submittedCount > 0){
                String msg = submittedCount + " manual attendance requests submitted successfully.";
                if (skippedCount > 0) {
                    msg += " (" + skippedCount + " dates skipped as they already have pending or approved requests).";
                }
                redirectAttrs.addFlashAttribute("successMessage", msg);
            } else if (skippedCount > 0) {
                redirectAttrs.addFlashAttribute("errorMessage", "All selected dates already have pending or approved attendance requests.");
            } else {
                redirectAttrs.addFlashAttribute("errorMessage", "No manual presentation transitions were detected to submit.");
            }
            
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", "Error submitting requests: " + e.getMessage());
        }
        
        return "redirect:/employee/intAttendance";
    }
}
