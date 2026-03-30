package com.maharecruitment.gov.in.attendance.controller;

import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
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

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/department")
public class AttendanceRegisterExternalEmployeeController {

    @Autowired
    private AttendanceRegisterService attendanceService;

    @Autowired
    private DepartmentRegistrationRepository departmentRegistrationRepository;

    @GetMapping("/extAttendance")
    public String attendanceRegisterExternalEmployee(Model model, HttpSession session) {
        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        List<DepartmentRegistrationEntity> departmentList = resolveAvailableDepartments(user);
        Long selectedDeptId = resolveSelectedDepartmentId(user, null, departmentList);
        populateDepartmentContext(model, departmentList, user, selectedDeptId);

        Map<Integer, String> monthNames = getMonthNames();
        LocalDate today = LocalDate.now();
        model.addAttribute("currentMonth", today.getMonthValue());
        model.addAttribute("currentYear", today.getYear());
        model.addAttribute("selectedMonth", today.getMonthValue());
        model.addAttribute("selectedYear", today.getYear());
        model.addAttribute("selectedMonthName", monthNames.get(today.getMonthValue()));
        model.addAttribute("monthNames", monthNames);
        model.addAttribute("daysInMonth", today.lengthOfMonth());
        model.addAttribute("isLocked", false);
        return "attendance/attendance-register-external";
    }

    @PostMapping("/fetchExternalAttendance")
    public String fetchExternalAttendance(@RequestParam(required = false) Long departmentId,
            @RequestParam int month,
            @RequestParam int year,
            Model model, HttpSession session) {

        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        List<DepartmentRegistrationEntity> departmentList = resolveAvailableDepartments(user);
        Long finalDeptId = resolveSelectedDepartmentId(user, departmentId, departmentList);
        populateDepartmentContext(model, departmentList, user, finalDeptId);

        List<AttendanceRegisterDTO> attendanceList = finalDeptId == null
                ? List.of()
                : attendanceService.getExternalAttendance(finalDeptId, month, year);
        model.addAttribute("attendanceList", attendanceList);

        Map<Integer, String> monthNames = getMonthNames();
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedMonthName", monthNames.get(month));
        model.addAttribute("monthNames", monthNames);

        YearMonth yearMonth = YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth();
        model.addAttribute("daysInMonth", daysInMonth);

        boolean isLocked = finalDeptId != null && attendanceService.isAttendanceLocked(finalDeptId, month, year);
        model.addAttribute("isLocked", isLocked);

        return "attendance/attendance-register-external";
    }

    private List<DepartmentRegistrationEntity> resolveAvailableDepartments(SessionUserDTO user) {
        if (user != null && user.departmentId() != null) {
            return departmentRegistrationRepository.findById(user.departmentId())
                    .map(List::of)
                    .orElse(List.of());
        }

        return departmentRegistrationRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        dept -> dept.getDepartmentName() == null ? "" : dept.getDepartmentName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Long resolveSelectedDepartmentId(SessionUserDTO user, Long departmentId,
            List<DepartmentRegistrationEntity> departmentList) {
        if (user != null && user.departmentId() != null) {
            return user.departmentId();
        }

        if (departmentId != null) {
            return departmentId;
        }

        if (departmentList.size() == 1) {
            return departmentList.get(0).getDepartmentRegistrationId();
        }

        return null;
    }

    private void populateDepartmentContext(Model model,
            List<DepartmentRegistrationEntity> departmentList,
            SessionUserDTO user,
            Long selectedDeptId) {
        model.addAttribute("departmentList", departmentList);
        model.addAttribute("departmentSelectionLocked", user != null && user.departmentId() != null);

        if (selectedDeptId == null) {
            return;
        }

        departmentList.stream()
                .filter(dept -> selectedDeptId.equals(dept.getDepartmentRegistrationId()))
                .findFirst()
                .or(() -> departmentRegistrationRepository.findById(selectedDeptId))
                .ifPresent(dept -> {
                    model.addAttribute("selectedDept", dept.getDepartmentName());
                    model.addAttribute("selectedDeptId", dept.getDepartmentRegistrationId());
                });
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
