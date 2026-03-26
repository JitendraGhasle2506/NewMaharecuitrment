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
        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        
        model.addAttribute("leaveApplication", new LeaveApplicationEntity());
        model.addAttribute("leaveTypes", leaveMasterRepository.findAll());
        return "attendance/apply-leave";
    }

    @GetMapping("/viewLeave")
    public String showLeaveHistory(Model model, HttpSession session) {
        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (user != null && user.employeeId() != null) {
            model.addAttribute("leaveHistory", leaveApplicationService.getLeaveApplicationsByEmployee(user.employeeId()));
        }
        return "attendance/view-leave";
    }

    @PostMapping("/submitLeave")
    public String submitLeaveApplication(@ModelAttribute("leaveApplication") LeaveApplicationEntity leaveApplication, 
            HttpSession session, RedirectAttributes redirectAttributes) {
        
        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (user == null || user.employeeId() == null) {
            redirectAttributes.addFlashAttribute("error", "Session expired or invalid user.");
            return "redirect:/login";
        }

        EmployeeEntity employee = employeeRepository.findById(user.employeeId()).orElse(null);
        if (employee == null) {
            redirectAttributes.addFlashAttribute("error", "Employee details not found.");
            return "redirect:/employee/applyLeave";
        }

        leaveApplication.setEmployeeId(employee.getEmployeeId());
        leaveApplicationService.saveLeaveApplication(leaveApplication);
        
        redirectAttributes.addFlashAttribute("success", "Leave application submitted successfully.");
        return "redirect:/employee/applyLeave";
    }
}
