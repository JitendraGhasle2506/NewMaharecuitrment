package com.maharecruitment.gov.in.web.controller.agency;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import com.maharecruitment.gov.in.master.dto.AgencyMasterResponse;
import com.maharecruitment.gov.in.web.service.master.AgencyMasterPageService;


@Controller
@RequestMapping("/agency/profile")
public class AgencyProfilePageController {

	private final AgencyMasterPageService agencyMasterPageService;

	public AgencyProfilePageController(AgencyMasterPageService agencyMasterPageService) {
		this.agencyMasterPageService = agencyMasterPageService;
	}


	@GetMapping
	public String profile(Principal principal, Model model,
			@ModelAttribute("agencyMasterResponse") AgencyMasterResponse agencyMasterResponse) {
		if (principal == null || principal.getName() == null) {
			return "redirect:/login";
		}
		agencyMasterResponse = agencyMasterPageService.getAgencyProfile(principal.getName());
		model.addAttribute("agencyMasterResponse", agencyMasterResponse);
		return "agency/profile";
	}

}
