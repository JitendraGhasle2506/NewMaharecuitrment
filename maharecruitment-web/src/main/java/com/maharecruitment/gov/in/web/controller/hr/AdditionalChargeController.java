package com.maharecruitment.gov.in.web.controller.hr;

import java.security.Principal;
import java.time.LocalDate;

import com.maharecruitment.gov.in.master.repository.CellMasterRepository;
import org.springframework.http.ResponseEntity;
import com.maharecruitment.gov.in.recruitment.repository.AdditionalChargeRepository;
import com.maharecruitment.gov.in.recruitment.service.AdditionalChargeService;
import com.maharecruitment.gov.in.recruitment.service.AdditionalChargeService.Request;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/hr/additional-charge")
@PreAuthorize("hasAuthority('ROLE_HR')")
public class AdditionalChargeController {
    private final AdditionalChargeService service;
    private final AdditionalChargeRepository repository;
    private final CellMasterRepository cells;

    public AdditionalChargeController(AdditionalChargeService service, AdditionalChargeRepository repository,
                                      CellMasterRepository cells) {
        this.service = service;
        this.repository = repository;
        this.cells = cells;
    }

    @GetMapping
    public String page(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("chargeForm", new Request("", "", "", LocalDate.now(), null, "", ""));
        return render(page, model);
    }

    @PostMapping
    public String assign(@Valid @ModelAttribute("chargeForm") Request request, BindingResult errors,
                         Principal principal, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                service.assign(request, principal.getName());
                redirect.addFlashAttribute("successMessage", "Additional charge assigned successfully.");
                return "redirect:/hr/additional-charge";
            } catch (IllegalArgumentException ex) {
                errors.reject("charge.invalid", ex.getMessage());
            }
        }
        return render(0, model);
    }

    @PostMapping("/{id}/revoke")
    public String revoke(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            service.revoke(id, principal.getName());
            redirect.addFlashAttribute("successMessage", "Additional charge revoked. The record remains in history.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/hr/additional-charge";
    }

    @GetMapping("/team-preview")
    @ResponseBody
    public ResponseEntity<?> preview(@RequestParam(defaultValue = "") String scope, @RequestParam String replaces) {
        try {
            return ResponseEntity.ok(service.previewTeam(scope, replaces).stream()
                    .map(person -> new TeamMember(person.getId(), person.getLabel())).toList());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        }
    }

    public record TeamMember(Long id, String label) { }

    private String render(int page, Model model) {
        model.addAttribute("sidebarActive", "Reporting Manager");
        model.addAttribute("employees", repository.employeeChoices());
        model.addAttribute("users", repository.userChoices());
        model.addAttribute("roles", repository.chargeRoles());
        model.addAttribute("replacementEmployees", repository.replacementEmployees());
        model.addAttribute("replacementUsers", repository.replacementUsers());
        model.addAttribute("cells", cells.findByActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCaseOrderByCellNameAsc("Y", "Y"));
        model.addAttribute("charges", service.list(page));
        return "hr/additional-charge";
    }
}
