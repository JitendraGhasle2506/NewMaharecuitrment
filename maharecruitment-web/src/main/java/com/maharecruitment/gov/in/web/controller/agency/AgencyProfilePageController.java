package com.maharecruitment.gov.in.web.controller.agency;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import com.maharecruitment.gov.in.auth.dto.SessionUserDTO;
import com.maharecruitment.gov.in.master.dto.AgencyMasterResponse;
import com.maharecruitment.gov.in.web.service.master.AgencyMasterPageService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/agency/profile")
public class AgencyProfilePageController {
	
	 private final AgencyMasterPageService agencyMasterPageService;

	    public AgencyProfilePageController(AgencyMasterPageService agencyMasterPageService) {
	        this.agencyMasterPageService = agencyMasterPageService;
	    }

	private static final String SESSION_USER_KEY = "SESSION_USER";

	@GetMapping
	public String profile(HttpSession session, Principal principal, Model model,@ModelAttribute("agencyMasterResponse")AgencyMasterResponse agencyMasterResponse) {
		SessionUserDTO sessionUser = extractSessionUser(session);
		System.out.println("principal" + principal.getName());
		agencyMasterResponse = agencyMasterPageService.getAgencyProfile(principal.getName());
		model.addAttribute("agencyMasterResponse",agencyMasterResponse);
		return "agency/profile";
	}

	private SessionUserDTO extractSessionUser(HttpSession session) {
		if (session == null) {
			return null;
		}
		Object sessionUser = session.getAttribute(SESSION_USER_KEY);
		if (sessionUser instanceof SessionUserDTO dto) {
			return dto;
		}
		return null;
	}
}
