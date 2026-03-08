package com.maharecruitment.gov.in.attendance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AttendanceController {
    @GetMapping("/attendance")
    public String home(Model model) {
        model.addAttribute("message", "Hello HRMS ATTENDANCE Module");
        return "attendance/attendance_home";
    }
    
    @GetMapping("/eservicebook/personal-details")
    public String personalDetails(Model model) {
        model.addAttribute("title", "Personal Details");
        return "attendance/personal_details";
    }

    @GetMapping("/eservicebook")
    public String eServiceBookHome(Model model) {
        model.addAttribute("message", "Hello HRMS E-Service Book Module");
        return "eservicebook/eservicebook_home";
    }
}
