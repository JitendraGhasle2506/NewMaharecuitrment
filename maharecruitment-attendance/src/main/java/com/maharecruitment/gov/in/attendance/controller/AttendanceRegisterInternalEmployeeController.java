package com.maharecruitment.gov.in.attendance.controller;

import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.maharecruitment.gov.in.attendance.dto.AttendanceRegisterDTO;
import com.maharecruitment.gov.in.attendance.dto.ManualAttendanceRequestDTO;
import java.util.List;
import java.util.stream.Collectors;
import com.maharecruitment.gov.in.attendance.service.AttendanceRegisterService;
import com.maharecruitment.gov.in.auth.dto.SessionUserDTO;
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

    @GetMapping("/intAttendance")
    public String myAttendance(Model model, HttpSession session) {
        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (user.employeeId() == null) {
            model.addAttribute("error", "Employee mapping not found in user account.");
            return "attendance/attendance-register-internal";
        }

        EmployeeEntity employee = employeeRepository.findById(user.employeeId())
                .orElse(null);

        if (employee == null) {
            model.addAttribute("error", "Employee details not found.");
            return "attendance/attendance-register-internal";
        }

        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year = today.getYear();

        AttendanceRegisterDTO attendance = attendanceService.getInternalAttendanceForEmployee(employee.getEmployeeId(), month, year);
        attendance.setDateRange(String.format("%02d-%d", month, year));
        
        model.addAttribute("attendance", attendance);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedYear", year);
        model.addAttribute("monthNames", getMonthNames());
        model.addAttribute("today", today);
        
        YearMonth yearMonth = YearMonth.of(year, month);
        model.addAttribute("daysInMonth", yearMonth.lengthOfMonth());
        
        List<ManualAttendanceRequestDTO> allRequests = attendanceService.getMyManualRequests(user.employeeId());
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

        return "attendance/attendance-register-internal";
    }

    @PostMapping("/fetchMyAttendance")
    public String fetchMyAttendance(@RequestParam(required = false) String dateRange,
            Model model, HttpSession session) {

        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (user.employeeId() == null) {
            model.addAttribute("error", "Employee mapping not found in user account.");
            return "attendance/attendance-register-internal";
        }

        EmployeeEntity employee = employeeRepository.findById(user.employeeId())
                .orElse(null);

        if (employee == null) {
            model.addAttribute("error", "Employee details not found.");
            return "attendance/attendance-register-internal";
        }
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

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

        AttendanceRegisterDTO attendance = attendanceService.getInternalAttendanceForEmployee(employee.getEmployeeId(), month, year);
        attendance.setDateRange(String.format("%02d-%d", month, year));
        
        model.addAttribute("attendance", attendance);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedYear", year);
        model.addAttribute("monthNames", getMonthNames());
        model.addAttribute("today", LocalDate.now());

        YearMonth yearMonth = YearMonth.of(year, month);
        model.addAttribute("daysInMonth", yearMonth.lengthOfMonth());
        
        List<ManualAttendanceRequestDTO> allRequests = attendanceService.getMyManualRequests(user.employeeId());
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

        return "attendance/attendance-register-internal";
    }

    private Map<Integer, String> getMonthNames() {
        Map<Integer, String> monthMap = new TreeMap<>();
        String[] months = new DateFormatSymbols().getMonths();
        for (int i = 0; i < 12; i++) {
            monthMap.put(i + 1, months[i]);
        }
        return monthMap;
    }

    @PostMapping("/manual-attendance/submit-bulk")
    public String submitBulkManualAttendance(
            @ModelAttribute AttendanceRegisterDTO dto,
            HttpSession session, 
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttrs) {
        
        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (user == null || user.employeeId() == null) {
            redirectAttrs.addFlashAttribute("errorMessage", "Employee mapping not found.");
            return "redirect:/employee/intAttendance";
        }
        
        try {
            int submittedCount = 0;
            int skippedCount = 0;
            if (dto.getAttendanceDays() != null) {
                for (com.maharecruitment.gov.in.attendance.dto.AttendanceDayDTO day : dto.getAttendanceDays()) {
                    if ("PRESENT".equalsIgnoreCase(day.getStatus())) {
                        String autoInTime = "10:00";
                        String autoOutTime = "18:00";
                        String reason = "Manual Attendance Mark from Dashboard Grid";
                        
                        if (attendanceService.submitManualAttendance(user.employeeId(), day.getDate(), autoInTime, autoOutTime, reason)) {
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
