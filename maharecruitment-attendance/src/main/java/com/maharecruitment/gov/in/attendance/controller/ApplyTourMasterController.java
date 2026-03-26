package com.maharecruitment.gov.in.attendance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.attendance.entity.TourApplicationEntity;
import com.maharecruitment.gov.in.attendance.service.TourApplicationService;
import com.maharecruitment.gov.in.auth.dto.SessionUserDTO;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/employee")
public class ApplyTourMasterController {

    @Autowired
    private TourApplicationService tourApplicationService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @GetMapping("/applyTour")
    public String showApplyTourForm(Model model, HttpSession session) {
        model.addAttribute("tourApplication", new TourApplicationEntity());
        return "attendance/apply-tour";
    }

    @GetMapping("/viewTour")
    public String showTourHistory(Model model, HttpSession session) {
        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (user != null && user.employeeId() != null) {
            model.addAttribute("tourHistory", tourApplicationService.getTourApplicationsByEmployee(user.employeeId()));
        }
        return "attendance/view-tour";
    }

    @PostMapping("/submitTour")
    public String submitTourApplication(@ModelAttribute("tourApplication") TourApplicationEntity tourApplication, 
            HttpSession session, RedirectAttributes redirectAttributes) {
        
        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (user == null || user.employeeId() == null) {
            redirectAttributes.addFlashAttribute("error", "Session expired or invalid user.");
            return "redirect:/login";
        }

        EmployeeEntity employee = employeeRepository.findById(user.employeeId()).orElse(null);
        if (employee == null) {
            redirectAttributes.addFlashAttribute("error", "Employee details not found.");
            return "redirect:/employee/applyTour";
        }

        tourApplication.setEmployeeId(employee.getEmployeeId());
        tourApplicationService.saveTourApplication(tourApplication);
        
        redirectAttributes.addFlashAttribute("success", "Tour application submitted successfully.");
        return "redirect:/employee/viewTour";
    }
}
