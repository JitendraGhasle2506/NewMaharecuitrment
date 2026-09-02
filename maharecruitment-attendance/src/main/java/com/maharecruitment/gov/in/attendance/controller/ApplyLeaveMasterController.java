package com.maharecruitment.gov.in.attendance.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;
import com.maharecruitment.gov.in.attendance.service.LeaveApplicationService;
import com.maharecruitment.gov.in.common.dto.SessionUserDTO;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.master.entity.LeaveEntity;
import com.maharecruitment.gov.in.master.repository.LeaveRepository;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/employee")
public class ApplyLeaveMasterController {

    private static final Map<String, Integer> PRIMARY_LEAVE_ORDER = Map.of(
            "EARNEDLEAVE", 0,
            "EL", 0,
            "CASUALLEAVE", 1,
            "CL", 1,
            "MEDICALLEAVE", 2,
            "ML", 2);
    private static final Set<String> EXCLUDED_LEAVE_NAMES = Set.of(
            "SPECIALLEAVE",
            "RESTRICTEDHOLIDAY",
            "HALFPAYLEAVE");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^A-Z0-9]");

    private final LeaveApplicationService leaveApplicationService;
    private final EmployeeRepository employeeRepository;
    private final LeaveRepository leaveMasterRepository;

    public ApplyLeaveMasterController(
            LeaveApplicationService leaveApplicationService,
            EmployeeRepository employeeRepository,
            LeaveRepository leaveMasterRepository) {
        this.leaveApplicationService = leaveApplicationService;
        this.employeeRepository = employeeRepository;
        this.leaveMasterRepository = leaveMasterRepository;
    }

    @GetMapping("/applyLeave")
    public String showApplyLeaveForm(Model model, HttpSession session) {
        populateApplyLeaveModel(model, session);
        if (!model.containsAttribute("leaveApplication")) {
            model.addAttribute("leaveApplication", new LeaveApplicationEntity());
        }
        return "attendance/apply-leave";
    }

    private void populateApplyLeaveModel(Model model, HttpSession session) {
        SessionUserDTO sessionUser = (SessionUserDTO) session.getAttribute("SESSION_USER");
        EmployeeEntity employee = null;
        if (sessionUser != null) {
            employee = requireEmployee(sessionUser);
        }
        populateApplyLeaveModel(model, employee, getSelectableLeaveTypes());
    }

    private void populateApplyLeaveModel(
            Model model,
            EmployeeEntity employee,
            List<LeaveEntity> leaveTypes) {
        if (employee != null) {
            model.addAttribute("employee", employee);
        }
        model.addAttribute("leaveTypes", leaveTypes);
    }

    private List<LeaveEntity> getSelectableLeaveTypes() {
        List<LeaveEntity> leaveTypes = leaveMasterRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(leave -> !isExcludedLeaveType(leave))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        boolean hasCompOff = leaveTypes.stream().anyMatch(this::isCompOffLeaveType);
        if (!hasCompOff) {
            LeaveEntity compOff = new LeaveEntity();
            compOff.setLeaveCode("CO");
            compOff.setLeaveName("Comp Off");
            leaveTypes.add(compOff);
        }
        leaveTypes.sort(Comparator
                .comparingInt(this::leaveTypePriority)
                .thenComparing(
                        leave -> safeValue(leave.getLeaveName()),
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(
                        leave -> safeValue(leave.getLeaveCode()),
                        String.CASE_INSENSITIVE_ORDER));
        return leaveTypes;
    }

    private int leaveTypePriority(LeaveEntity leave) {
        Integer namePriority = PRIMARY_LEAVE_ORDER.get(normalizeLeaveValue(leave.getLeaveName()));
        if (namePriority != null) {
            return namePriority;
        }
        return PRIMARY_LEAVE_ORDER.getOrDefault(normalizeLeaveValue(leave.getLeaveCode()), 3);
    }

    private boolean isExcludedLeaveType(LeaveEntity leave) {
        return EXCLUDED_LEAVE_NAMES.contains(normalizeLeaveValue(leave.getLeaveName()));
    }

    private boolean isSelectableLeaveType(String submittedLeaveType, List<LeaveEntity> leaveTypes) {
        String normalizedValue = normalizeLeaveValue(submittedLeaveType);
        return !normalizedValue.isEmpty() && leaveTypes.stream()
                .anyMatch(leave -> normalizedValue.equals(normalizeLeaveValue(leave.getLeaveCode())));
    }

    private boolean isCompOffLeaveType(LeaveEntity leave) {
        if (leave == null) {
            return false;
        }
        String code = normalizeLeaveValue(leave.getLeaveCode());
        String name = normalizeLeaveValue(leave.getLeaveName());
        return code.equals("CO")
                || code.equals("COMPOFF")
                || name.equals("CO")
                || name.equals("COMPOFF")
                || name.equals("COMPENSATORYOFF");
    }

    private String normalizeLeaveValue(String value) {
        return NON_ALPHANUMERIC.matcher(safeValue(value).toUpperCase(Locale.ROOT)).replaceAll("");
    }

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }

    @GetMapping("/viewLeave")
    public String showLeaveHistory(Model model, HttpSession session) {
        SessionUserDTO sessionUser = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (sessionUser != null) {
            EmployeeEntity employee = requireEmployee(sessionUser);
            model.addAttribute("leaveHistory", leaveApplicationService.getLeaveApplicationsByEmployee(employee.getEmployeeId()));
        }
        return "attendance/view-leave";
    }

    @GetMapping("/validateCompOffWorkedDate")
    @ResponseBody
    public Map<String, Object> validateCompOffWorkedDate(
            @RequestParam("workedDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate workedDate,
            HttpSession session) {
        SessionUserDTO sessionUser = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (sessionUser == null) {
            return Map.of(
                    "valid", false,
                    "message", "Session expired or invalid user.");
        }

        EmployeeEntity employee = requireEmployee(sessionUser);
        boolean valid = leaveApplicationService.isValidCompOffWorkedDate(employee.getEmployeeId(), workedDate);
        return Map.of(
                "valid", valid,
                "message", valid
                        ? "Worked date verified."
                        : "Comp-off worked date is allowed only when you were present or had an approved tour on that date.");
    }

    @PostMapping("/submitLeave")
    public String submitLeaveApplication(@Valid @ModelAttribute("leaveApplication") LeaveApplicationEntity leaveApplication,
            BindingResult bindingResult, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        
        SessionUserDTO sessionUser = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (sessionUser == null) {
            redirectAttributes.addFlashAttribute("error", "Session expired or invalid user.");
            return "redirect:/login";
        }

        EmployeeEntity employee = requireEmployee(sessionUser);

        leaveApplication.setEmployeeId(employee.getEmployeeId());
        List<LeaveEntity> leaveTypes = getSelectableLeaveTypes();
        if (bindingResult.hasErrors()) {
            populateApplyLeaveModel(model, employee, leaveTypes);
            return "attendance/apply-leave";
        }

        if (!isSelectableLeaveType(leaveApplication.getLeaveType(), leaveTypes)) {
            populateApplyLeaveModel(model, employee, leaveTypes);
            model.addAttribute("error", "Please select an available leave type.");
            return "attendance/apply-leave";
        }

        try {
            leaveApplicationService.saveLeaveApplication(leaveApplication);
        } catch (IllegalArgumentException ex) {
            populateApplyLeaveModel(model, employee, leaveTypes);
            model.addAttribute("error", ex.getMessage());
            return "attendance/apply-leave";
        }
        
        redirectAttributes.addFlashAttribute("success", "Leave application submitted successfully.");
        return "redirect:/employee/applyLeave";
    }

    @PostMapping("/cancelLeave")
    public String cancelLeaveApplication(@RequestParam("leaveId") Long leaveId,
            HttpSession session, RedirectAttributes redirectAttributes) {
        SessionUserDTO sessionUser = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (sessionUser == null) {
            redirectAttributes.addFlashAttribute("error", "Session expired or invalid user.");
            return "redirect:/login";
        }

        EmployeeEntity employee = requireEmployee(sessionUser);
        try {
            leaveApplicationService.cancelLeaveApplication(leaveId, employee.getEmployeeId());
            redirectAttributes.addFlashAttribute("success", "Leave application cancelled successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/employee/viewLeave";
    }

    private EmployeeEntity requireEmployee(SessionUserDTO sessionUser) {
        return employeeRepository.findByUser_Id(sessionUser.id())
                .orElseThrow(() -> new IllegalArgumentException("Employee record not found"));
    }
}
