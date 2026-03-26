package com.maharecruitment.gov.in.attendance.controller;

import com.maharecruitment.gov.in.attendance.dto.AttendanceRegisterDTO;
import com.maharecruitment.gov.in.attendance.service.AttendanceRegisterService;
import com.maharecruitment.gov.in.auth.dto.SessionUserDTO;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.text.DateFormatSymbols;
import java.util.Map;
import java.util.TreeMap;

@Controller
@RequestMapping("/hod1/team-attendance")
public class TeamAttendanceController {

    @Autowired
    private AttendanceRegisterService attendanceService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @GetMapping
    public String listTeam(Model model, HttpSession session, @RequestParam(required = false) String roleType) {
        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (user == null) return "redirect:/login";

        if (roleType == null) {
            roleType = (user.roles() != null && user.roles().contains("ROLE_HOD")) ? "HOD" : "MANAGER";
        }

        Long approverId = "HOD".equalsIgnoreCase(roleType) ? user.id() : user.employeeId();
        model.addAttribute("teamMembers", attendanceService.getTeamMembers(approverId, roleType));
        model.addAttribute("currentRoleType", roleType);

        return "attendance/team-attendance-list";
    }

    @GetMapping("/view")
    public String viewMemberAttendance(
            @RequestParam("empId") Long empId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Model model, HttpSession session) {
        
        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (user == null) return "redirect:/login";

        EmployeeEntity employee = employeeRepository.findById(empId).orElse(null);
        if (employee == null) {
            model.addAttribute("error", "Employee not found");
            return "attendance/team-attendance-list";
        }

        LocalDate today = LocalDate.now();
        if (month == null) month = today.getMonthValue();
        if (year == null) year = today.getYear();

        AttendanceRegisterDTO attendance = attendanceService.getInternalAttendanceForEmployee(empId, month, year);
        attendance.setDateRange(String.format("%02d-%d", month, year));

        model.addAttribute("attendance", attendance);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedYear", year);
        model.addAttribute("monthNames", getMonthNames());
        model.addAttribute("daysInMonth", YearMonth.of(year, month).lengthOfMonth());
        model.addAttribute("targetEmployee", employee);

        return "attendance/view-employee-attendance";
    }

    private Map<Integer, String> getMonthNames() {
        Map<Integer, String> monthMap = new TreeMap<>();
        String[] months = new DateFormatSymbols().getMonths();
        for (int i = 0; i < 12; i++) {
            if (!months[i].isEmpty()) monthMap.put(i + 1, months[i]);
        }
        return monthMap;
    }
}
