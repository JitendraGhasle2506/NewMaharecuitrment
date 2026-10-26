package com.maharecruitment.gov.in.web.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.maharecruitment.gov.in.web.dto.pip.*;
import com.maharecruitment.gov.in.web.service.pip.EmployeePipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/reporting-manager/pip")
@PreAuthorize(EmployeePipService.AUTHORIZATION)
@RequiredArgsConstructor
public class EmployeePipController {
    private final EmployeePipService service;

    @GetMapping
    public String board(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page,
            Principal principal, Model model) {
        if (!model.containsAttribute("startForm")) {
            var form = new PipStartForm();
            form.setStartDate(today());
            model.addAttribute("startForm", form);
        }
        model.addAttribute("board", service.board(principal.getName(), q, page));
        model.addAttribute("q", q);
        model.addAttribute("today", today());
        return "employee/pip-board";
    }

    @PostMapping
    public String start(@Valid @ModelAttribute("startForm") PipStartForm form, BindingResult errors,
            Principal principal, Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) return board("", 0, principal, model);
        try {
            Long id = service.start(principal.getName(), form.getEmployeeId(), form.getStartDate(), form.getMonths(), form.getObjectives(), form.getReason());
            redirect.addFlashAttribute("successMessage", "PIP started. The employee remains active while under review.");
            return "redirect:/reporting-manager/pip/" + id;
        } catch (ResponseStatusException ex) {
            rejectBusinessError(ex, errors);
            return board("", 0, principal, model);
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("detail", service.detail(principal.getName(), id));
        model.addAttribute("today", today());
        if (!model.containsAttribute("reviewForm")) {
            var form = new PipReviewForm();
            form.setReviewDate(today());
            model.addAttribute("reviewForm", form);
        }
        if (!model.containsAttribute("decisionForm")) model.addAttribute("decisionForm", new PipDecisionForm());
        return "employee/pip-detail";
    }

    @PostMapping("/{id}/reviews")
    public String review(@PathVariable Long id, @Valid @ModelAttribute("reviewForm") PipReviewForm form, BindingResult errors,
            Principal principal, Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) return detail(id, principal, model);
        try {
            service.review(principal.getName(), id, form.getReviewDate(), form.getRating(), form.getFeedback());
        } catch (ResponseStatusException ex) {
            rejectBusinessError(ex, errors);
            return detail(id, principal, model);
        }
        redirect.addFlashAttribute("successMessage", "Performance review added to the feedback history.");
        return "redirect:/reporting-manager/pip/" + id;
    }

    @PostMapping("/{id}/decision")
    public String decide(@PathVariable Long id, @Valid @ModelAttribute("decisionForm") PipDecisionForm form, BindingResult errors,
            Principal principal, Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) return detail(id, principal, model);
        try {
            service.decide(principal.getName(), id, form.getDecision(), form.getFeedback(), form.getExitDate());
        } catch (ResponseStatusException ex) {
            rejectBusinessError(ex, errors);
            return detail(id, principal, model);
        }
        redirect.addFlashAttribute("successMessage", "PIP outcome saved successfully.");
        return "redirect:/reporting-manager/pip/" + id;
    }

    private void rejectBusinessError(ResponseStatusException ex, BindingResult errors) {
        // Ownership and authentication failures must retain their HTTP status and disclose no employee data.
        if (ex.getStatusCode() != HttpStatus.BAD_REQUEST && ex.getStatusCode() != HttpStatus.CONFLICT) throw ex;
        errors.reject("pip.invalid", ex.getReason());
    }
    private LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Kolkata")); }
}
