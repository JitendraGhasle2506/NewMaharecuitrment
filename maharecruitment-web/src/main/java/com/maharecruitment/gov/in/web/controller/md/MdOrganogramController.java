package com.maharecruitment.gov.in.web.controller.md;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.maharecruitment.gov.in.master.service.OrganogramService;

@Controller
@RequestMapping("/md")
public class MdOrganogramController {

    private final OrganogramService organogramService;

    public MdOrganogramController(OrganogramService organogramService) {
        this.organogramService = organogramService;
    }

    @GetMapping("/organogram")
    public String organogram(Model model) {
        model.addAttribute("organogram", organogramService.getActiveOrganogram());
        return "md/organogram";
    }
}
