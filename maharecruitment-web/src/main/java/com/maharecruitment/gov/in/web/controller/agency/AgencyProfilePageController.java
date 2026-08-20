package com.maharecruitment.gov.in.web.controller.agency;

import java.security.Principal;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.common.util.SensitiveDataMaskingUtil;
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
		maskSensitiveIdentifiers(agencyMasterResponse);
		model.addAttribute("agencyMasterResponse", agencyMasterResponse);
		return "agency/profile";
	}

	@GetMapping("/sensitive-identifiers/{identifier}")
	@ResponseBody
	public ResponseEntity<SensitiveIdentifierResponse> revealSensitiveIdentifier(
			@PathVariable String identifier,
			Principal principal) {
		if (principal == null || !StringUtils.hasText(principal.getName())) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		AgencyMasterResponse profile = agencyMasterPageService.getAgencyProfile(principal.getName());
		String value = switch (identifier.toUpperCase(java.util.Locale.ROOT)) {
			case "PAN" -> profile.getPanNumber();
			case "GST" -> profile.getGstNumber();
			case "CERTIFICATE" -> profile.getCertificateNumber();
			default -> null;
		};
		if (!StringUtils.hasText(value)) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(new SensitiveIdentifierResponse(value.trim()));
	}

	private void maskSensitiveIdentifiers(AgencyMasterResponse profile) {
		profile.setPanNumber(SensitiveDataMaskingUtil.maskPan(profile.getPanNumber()));
		profile.setGstNumber(SensitiveDataMaskingUtil.maskGst(profile.getGstNumber()));
		profile.setCertificateNumber(
				SensitiveDataMaskingUtil.maskKeepingLastFour(profile.getCertificateNumber()));
	}

	public record SensitiveIdentifierResponse(String value) {
	}

}
