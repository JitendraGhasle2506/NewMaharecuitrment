package com.maharecruitment.gov.in.attendance.controller;

import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.maharecruitment.gov.in.attendance.dto.AttendanceRegisterDTO;
import com.maharecruitment.gov.in.attendance.dto.AttendanceRegisterWrapperDTO;
import com.maharecruitment.gov.in.attendance.service.AttendanceRegisterService;
import com.maharecruitment.gov.in.auth.dto.SessionUserDTO;
import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.repository.DepartmentRegistrationRepository;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;
import com.maharecruitment.gov.in.master.repository.DepartmentMstRepository;
import com.maharecruitment.gov.in.master.service.DepartmentMstService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/department")
public class AttendanceRegisterExternalEmployeeController {

    @Autowired
    private AttendanceRegisterService attendanceService;

    @Autowired
    private DepartmentMstService departmentMstService;

    @Autowired
    private DepartmentMstRepository departmentMstRepository;
    @Autowired
    private DepartmentRegistrationRepository departmentRegistrationRepository;

    @GetMapping("/extAttendance")
    public String attendanceRegisterExternalEmployee(Model model, HttpSession session) {
        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (user != null && user.departmentId() != null) {
            DepartmentRegistrationEntity dept = departmentRegistrationRepository.findById(user.departmentId())
                    .orElse(null);
            if (dept != null) {
                DepartmentMst department = departmentMstRepository.findById(dept.getDepartmentId())
                        .orElse(null);

                model.addAttribute("departmentList", List.of(department));
                model.addAttribute("selectedDept", department.getDepartmentName());
            }
        } else {
            model.addAttribute("departmentList", departmentMstService.getAllDepartment());
        }

        LocalDate today = LocalDate.now();
        model.addAttribute("currentMonth", today.getMonthValue());
        model.addAttribute("currentYear", today.getYear());
        model.addAttribute("monthNames", getMonthNames());
        return "attendance/attendance-register-external";
    }

    @PostMapping("/fetchExternalAttendance")
    public String fetchExternalAttendance(@RequestParam(required = false) Long departmentId,
            @RequestParam int month,
            @RequestParam int year,
            Model model, HttpSession session) {

        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        Long finalDeptId = (user != null && user.departmentId() != null) ? user.departmentId() : departmentId;

        if (user != null && user.departmentId() != null) {
            DepartmentRegistrationEntity dept = departmentRegistrationRepository.findById(finalDeptId)
                    .orElse(null);
            if (dept != null) {
                DepartmentMst department = departmentMstRepository.findById(dept.getDepartmentId())
                        .orElse(null);
                model.addAttribute("departmentList", List.of(department));
                model.addAttribute("selectedDept", department.getDepartmentName());
                model.addAttribute("selectedDeptId", finalDeptId);
            }
        } else {
            model.addAttribute("departmentList", departmentMstService.getAllDepartment());
            if (finalDeptId != null) {
                final Long currentDeptId = finalDeptId;
                departmentMstRepository.findById(currentDeptId).ifPresent(d -> {
                    model.addAttribute("selectedDept", d.getDepartmentName());
                    model.addAttribute("selectedDeptId", currentDeptId);
                });
            }
        }

        List<AttendanceRegisterDTO> attendanceList = attendanceService.getExternalAttendance(finalDeptId, month, year);
        model.addAttribute("attendanceList", attendanceList);

        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedYear", year);
        model.addAttribute("monthNames", getMonthNames());

        YearMonth yearMonth = YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth();
        model.addAttribute("daysInMonth", daysInMonth);

        // Use the existing lock check logic from service or set a default if not fully
        // implemented yet
        boolean isLocked = attendanceService.isAttendanceLocked(finalDeptId, month, year);
        model.addAttribute("isLocked", isLocked);

        return "attendance/attendance-register-external";
    }

    private Map<Integer, String> getMonthNames() {
        Map<Integer, String> monthMap = new TreeMap<>();
        String[] months = new DateFormatSymbols().getMonths();
        for (int i = 0; i < 12; i++) {
            monthMap.put(i + 1, months[i]);
        }
        return monthMap;
    }

    @PostMapping("/saveExternalAttendance")
    @ResponseBody
    public String saveExternalAttendance(
            @ModelAttribute AttendanceRegisterWrapperDTO wrapper) {

        List<AttendanceRegisterDTO> dtos = wrapper.getDtos();

        if (dtos != null) {
            attendanceService.saveExternalAttendance(dtos);
        }

        return "Success";
    }

    @PostMapping("/lockAttendance")
    @ResponseBody
    public String lockAttendance(@RequestParam int month, @RequestParam int year, HttpSession session) {
        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (user != null && user.departmentId() != null) {
            attendanceService.lockAttendance(user.departmentId(), month, year);
            return "Success";
        }
        return "Error: Session expired";
    }
}
