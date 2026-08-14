package com.maharecruitment.gov.in.attendance.controller;

import java.text.DateFormatSymbols;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.maharecruitment.gov.in.attendance.dto.AttendanceCalendarDayDTO;
import com.maharecruitment.gov.in.attendance.dto.AttendanceDayDTO;
import com.maharecruitment.gov.in.attendance.dto.AttendanceRegisterDTO;
import com.maharecruitment.gov.in.attendance.dto.EmployeeAttendanceRequestDTO;
import com.maharecruitment.gov.in.attendance.dto.ManualAttendanceRequestDTO;
import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;
import com.maharecruitment.gov.in.attendance.entity.TourApplicationEntity;
import com.maharecruitment.gov.in.attendance.service.AttendanceRegisterService;
import com.maharecruitment.gov.in.attendance.service.HolidayService;
import com.maharecruitment.gov.in.attendance.service.LeaveApplicationService;
import com.maharecruitment.gov.in.attendance.service.TourApplicationService;
import com.maharecruitment.gov.in.attendance.service.WeekOffWorkingDayService;
import com.maharecruitment.gov.in.common.dto.SessionUserDTO;
import com.maharecruitment.gov.in.common.util.SensitiveDataMaskingUtil;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/employee")
public class AttendanceRegisterInternalEmployeeController {

    private static final String SESSION_USER_KEY = "SESSION_USER";

    @Autowired
    private AttendanceRegisterService attendanceService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private HolidayService holidayService;

    @Autowired
    private WeekOffWorkingDayService weekOffWorkingDayService;

    @Autowired
    private LeaveApplicationService leaveApplicationService;

    @Autowired
    private TourApplicationService tourApplicationService;

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

    @PostMapping(value = "/intAttendance/aadhaar/reveal", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public ResponseEntity<String> revealMyAadhaar(HttpSession session) {
        SessionUserDTO sessionUser = session != null
                ? (SessionUserDTO) session.getAttribute(SESSION_USER_KEY)
                : null;
        if (sessionUser == null || sessionUser.id() == null) {
            return sensitiveResponse(HttpStatus.UNAUTHORIZED, null);
        }

        EmployeeEntity employee = employeeRepository.findByUser_Id(sessionUser.id()).orElse(null);
        if (employee == null) {
            return sensitiveResponse(HttpStatus.FORBIDDEN, null);
        }

        String normalizedAadhaar = SensitiveDataMaskingUtil.normalizeAadhaar(employee.getAadhaarNumber());
        if (!StringUtils.hasText(normalizedAadhaar) || !normalizedAadhaar.matches("\\d{12}")) {
            return sensitiveResponse(HttpStatus.NOT_FOUND, null);
        }

        return sensitiveResponse(HttpStatus.OK, normalizedAadhaar);
    }

    private ResponseEntity<String> sensitiveResponse(HttpStatus status, String body) {
        return ResponseEntity.status(status)
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
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

        boolean externalEmployee = "EXTERNAL".equalsIgnoreCase(employee.getRecruitmentType());
        model.addAttribute("externalEmployee", externalEmployee);
        model.addAttribute("employeeDepartment", externalEmployee ? resolveDepartmentName(employee) : null);
        model.addAttribute("employeeSubDepartment", externalEmployee ? resolveSubDepartmentName(employee) : null);

        YearMonth yearMonth = YearMonth.of(year, month);
        model.addAttribute("daysInMonth", yearMonth.lengthOfMonth());

        populateEmployeeRequests(model, employee.getEmployeeId());
    }

    private void populateEmployeeRequests(Model model, Long employeeId) {
        List<ManualAttendanceRequestDTO> manualRequests =
                safeList(attendanceService.getMyManualRequests(employeeId));
        List<LeaveApplicationEntity> leaveRequests =
                safeList(leaveApplicationService.getLeaveApplicationsByEmployee(employeeId));
        List<TourApplicationEntity> tourRequests =
                safeList(tourApplicationService.getTourApplicationsByEmployee(employeeId));

        List<EmployeeAttendanceRequestDTO> requests = new ArrayList<>(
                manualRequests.size() + leaveRequests.size() + tourRequests.size());
        manualRequests.forEach(request -> requests.add(toEmployeeRequest(request)));
        leaveRequests.forEach(request -> requests.add(toEmployeeRequest(request)));
        tourRequests.forEach(request -> requests.add(toEmployeeRequest(request)));
        requests.sort(Comparator
                .comparing(
                        EmployeeAttendanceRequestDTO::getSubmittedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(
                        EmployeeAttendanceRequestDTO::getStartDate,
                        Comparator.nullsLast(Comparator.reverseOrder())));

        List<EmployeeAttendanceRequestDTO> pendingRequests = new ArrayList<>();
        List<EmployeeAttendanceRequestDTO> approvedRequests = new ArrayList<>();
        List<EmployeeAttendanceRequestDTO> rejectedRequests = new ArrayList<>();
        for (EmployeeAttendanceRequestDTO request : requests) {
            switch (request.getStatus()) {
                case "APPROVED" -> approvedRequests.add(request);
                case "REJECTED" -> rejectedRequests.add(request);
                default -> pendingRequests.add(request);
            }
        }

        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("approvedRequests", approvedRequests);
        model.addAttribute("rejectedRequests", rejectedRequests);
    }

    private EmployeeAttendanceRequestDTO toEmployeeRequest(ManualAttendanceRequestDTO request) {
        EmployeeAttendanceRequestDTO view = new EmployeeAttendanceRequestDTO();
        view.setRequestType("MANUAL_ATTENDANCE");
        view.setRequestTypeLabel("Manual Attendance");
        view.setSubmittedAt(request.getCreatedAt());
        view.setStartDate(request.getAttendanceDate());
        view.setEndDate(request.getAttendanceDate());
        view.setCategory("Attendance correction");
        view.setDetails(textOrFallback(request.getReason(), "Manual attendance request"));
        view.setInTime(request.getInTime());
        view.setOutTime(request.getOutTime());
        view.setStatus(normalizeRequestStatus(request.getHodStatus()));
        view.setReviewerRemarks(firstText(request.getHodComments(), request.getManagerComments()));
        return view;
    }

    private EmployeeAttendanceRequestDTO toEmployeeRequest(LeaveApplicationEntity request) {
        EmployeeAttendanceRequestDTO view = new EmployeeAttendanceRequestDTO();
        view.setRequestType("LEAVE");
        view.setRequestTypeLabel("Leave");
        view.setSubmittedAt(request.getApplicationDate());
        view.setStartDate(request.getStartDate());
        view.setEndDate(request.getEndDate());
        view.setCategory(joinLabels(request.getLeaveType(), request.getLeaveCategory(), "Leave request"));
        view.setDetails(textOrFallback(request.getDescription(), "Leave application"));
        view.setStatus(normalizeRequestStatus(request.getStatus()));
        view.setReviewerRemarks(firstText(request.getHodRemarks(), request.getManagerRemarks()));
        return view;
    }

    private EmployeeAttendanceRequestDTO toEmployeeRequest(TourApplicationEntity request) {
        EmployeeAttendanceRequestDTO view = new EmployeeAttendanceRequestDTO();
        view.setRequestType("TOUR");
        view.setRequestTypeLabel("Tour");
        view.setSubmittedAt(request.getApplicationDate());
        view.setStartDate(request.getStartDate());
        view.setEndDate(request.getEndDate());
        view.setCategory(joinLabels(request.getTourCategory(), request.getTimePeriod(), "Tour request"));
        view.setDetails(textOrFallback(request.getDescription(), "Tour application"));
        view.setStatus(normalizeRequestStatus(request.getStatus()));
        view.setReviewerRemarks(textOrFallback(request.getHodRemarks(), null));
        return view;
    }

    private String normalizeRequestStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "PENDING";
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("APPROVED".equals(normalized) || "ACCEPTED".equals(normalized)) {
            return "APPROVED";
        }
        if ("REJECTED".equals(normalized) || "CANCELLED".equals(normalized)) {
            return "REJECTED";
        }
        return "PENDING";
    }

    private String joinLabels(String first, String second, String fallback) {
        boolean hasFirst = StringUtils.hasText(first);
        boolean hasSecond = StringUtils.hasText(second);
        if (hasFirst && hasSecond) {
            return first.trim() + " / " + second.trim();
        }
        return hasFirst ? first.trim() : textOrFallback(second, fallback);
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first.trim() : textOrFallback(second, null);
    }

    private String textOrFallback(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
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

    private String resolveDepartmentName(EmployeeEntity employee) {
        if (employee.getDepartmentRegistration() != null
                && StringUtils.hasText(employee.getDepartmentRegistration().getDepartmentName())) {
            return employee.getDepartmentRegistration().getDepartmentName();
        }
        if (employee.getDepartment() != null
                && StringUtils.hasText(employee.getDepartment().getDepartmentName())) {
            return employee.getDepartment().getDepartmentName();
        }
        return "-";
    }

    private String resolveSubDepartmentName(EmployeeEntity employee) {
        if (employee.getSubDepartment() == null
                || !StringUtils.hasText(employee.getSubDepartment().getSubDeptName())) {
            return null;
        }
        return employee.getSubDepartment().getSubDeptName();
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
        SessionUserDTO sessionUser = (SessionUserDTO) session.getAttribute(SESSION_USER_KEY);
        return employeeRepository.findByUser_Id(sessionUser.id())
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
        
        EmployeeEntity employee = resolveCurrentEmployee(session);

        
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
