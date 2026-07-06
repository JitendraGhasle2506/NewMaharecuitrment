package com.maharecruitment.gov.in.attendance.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private LeaveApplicationService leaveApplicationService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveRepository leaveMasterRepository;

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
        if (sessionUser != null) {
            EmployeeEntity employee = employeeRepository.findByEmail(sessionUser.email())
                    .orElseThrow(() -> new IllegalArgumentException("Employee record not found"));
            model.addAttribute("employee", employee);
        }
        model.addAttribute("leaveTypes", getLeaveTypesWithCompOff());
    }

    private List<LeaveEntity> getLeaveTypesWithCompOff() {
        List<LeaveEntity> leaveTypes = new ArrayList<>(leaveMasterRepository.findAll());
        boolean hasCompOff = leaveTypes.stream().anyMatch(this::isCompOffLeaveType);
        if (!hasCompOff) {
            LeaveEntity compOff = new LeaveEntity();
            compOff.setLeaveCode("CO");
            compOff.setLeaveName("Comp Off");
            leaveTypes.add(compOff);
        }
        leaveTypes.sort(Comparator.comparing(
                leave -> safeValue(leave.getLeaveName()),
                String.CASE_INSENSITIVE_ORDER));
        return leaveTypes;
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
        return safeValue(value).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }

    @GetMapping("/viewLeave")
    public String showLeaveHistory(Model model, HttpSession session) {
        SessionUserDTO sessionUser = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (sessionUser != null) {
            EmployeeEntity employee = employeeRepository.findByEmail(sessionUser.email())
                    .orElseThrow(() -> new IllegalArgumentException("Employee record not found"));
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

        EmployeeEntity employee = employeeRepository.findByEmail(sessionUser.email())
                .orElseThrow(() -> new IllegalArgumentException("Employee record not found"));
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

        EmployeeEntity employee = employeeRepository.findByEmail(sessionUser.email())
                .orElseThrow(() -> new IllegalArgumentException("Employee record not found"));

        leaveApplication.setEmployeeId(employee.getEmployeeId());
        if (bindingResult.hasErrors()) {
            populateApplyLeaveModel(model, session);
            return "attendance/apply-leave";
        }

        try {
            leaveApplicationService.saveLeaveApplication(leaveApplication);
        } catch (IllegalArgumentException ex) {
            populateApplyLeaveModel(model, session);
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

        EmployeeEntity employee = employeeRepository.findByEmail(sessionUser.email())
                .orElseThrow(() -> new IllegalArgumentException("Employee record not found"));
        try {
            leaveApplicationService.cancelLeaveApplication(leaveId, employee.getEmployeeId());
            redirectAttributes.addFlashAttribute("success", "Leave application cancelled successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/employee/viewLeave";
    }
}
