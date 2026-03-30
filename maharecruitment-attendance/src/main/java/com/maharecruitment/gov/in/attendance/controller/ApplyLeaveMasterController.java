package com.maharecruitment.gov.in.attendance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;
import com.maharecruitment.gov.in.attendance.service.LeaveApplicationService;
import com.maharecruitment.gov.in.auth.dto.SessionUserDTO;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.master.repository.LeaveRepository;

import jakarta.servlet.http.HttpSession;

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
        SessionUserDTO sessionUser = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (sessionUser != null) {
            EmployeeEntity employee = employeeRepository.findByEmail(sessionUser.email())
                    .orElseThrow(() -> new IllegalArgumentException("Employee record not found"));
            model.addAttribute("employee", employee);
        }
        
        model.addAttribute("leaveApplication", new LeaveApplicationEntity());
        model.addAttribute("leaveTypes", leaveMasterRepository.findAll());
        return "attendance/apply-leave";
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

    @PostMapping("/submitLeave")
    public String submitLeaveApplication(@ModelAttribute("leaveApplication") LeaveApplicationEntity leaveApplication, 
            HttpSession session, RedirectAttributes redirectAttributes) {
        
        SessionUserDTO sessionUser = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (sessionUser == null) {
            redirectAttributes.addFlashAttribute("error", "Session expired or invalid user.");
            return "redirect:/login";
        }

        EmployeeEntity employee = employeeRepository.findByEmail(sessionUser.email())
                .orElseThrow(() -> new IllegalArgumentException("Employee record not found"));

        leaveApplication.setEmployeeId(employee.getEmployeeId());
        leaveApplicationService.saveLeaveApplication(leaveApplication);
        
        redirectAttributes.addFlashAttribute("success", "Leave application submitted successfully.");
        return "redirect:/employee/applyLeave";
    }
}
