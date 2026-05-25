package com.maharecruitment.gov.in.invoice.controller;

import java.time.YearMonth;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.invoice.dto.AgencyMonthlyBillGenerateRequest;
import com.maharecruitment.gov.in.invoice.dto.AgencyMonthlyBillListItemView;
import com.maharecruitment.gov.in.invoice.entity.AgencyMonthlyBillEmployeeType;
import com.maharecruitment.gov.in.invoice.service.AgencyMonthlyBillService;
import com.maharecruitment.gov.in.master.entity.AgencyStatus;
import com.maharecruitment.gov.in.master.repository.AgencyMasterRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/auditor/agency-monthly-bills")
public class AuditorAgencyMonthlyBillController {

    private final AgencyMonthlyBillService billService;
    private final AgencyMasterRepository agencyMasterRepository;

    public AuditorAgencyMonthlyBillController(
            AgencyMonthlyBillService billService,
            AgencyMasterRepository agencyMasterRepository) {
        this.billService = billService;
        this.agencyMasterRepository = agencyMasterRepository;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Order.desc("generatedDate"), Sort.Order.desc("agencyMonthlyBillId")));
        Page<AgencyMonthlyBillListItemView> bills = billService.getGeneratedBills(pageable);
        model.addAttribute("bills", bills);
        return "invoice/agency-monthly-bill-list";
    }
    @GetMapping("/new")
    public String generateForm(Model model) {
        YearMonth defaultPeriod = YearMonth.now().minusMonths(1);
        AgencyMonthlyBillGenerateRequest request = new AgencyMonthlyBillGenerateRequest();
        request.setMonth(defaultPeriod.getMonthValue());
        request.setYear(defaultPeriod.getYear());
        request.setEmployeeType(AgencyMonthlyBillEmployeeType.ALL);
        populateForm(model, request);
        return "invoice/agency-monthly-bill-form";
    }

    @PostMapping("/preview")
    public String preview(
            @Valid @ModelAttribute("billRequest") AgencyMonthlyBillGenerateRequest request,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            populateForm(model, request);
            return "invoice/agency-monthly-bill-form";
        }

        try {
            model.addAttribute("billPreview", billService.preview(request));
            model.addAttribute("previewReady", true);
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        }
        populateForm(model, request);
        return "invoice/agency-monthly-bill-form";
    }

    @PostMapping("/generate")
    public String generate(
            @Valid @ModelAttribute("billRequest") AgencyMonthlyBillGenerateRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, request);
            return "invoice/agency-monthly-bill-form";
        }

        try {
            var bill = billService.generate(request, resolveActorEmail());
            redirectAttributes.addFlashAttribute("successMessage", "Agency monthly bill generated successfully");
            return "redirect:/auditor/agency-monthly-bills/" + bill.getAgencyMonthlyBillId();
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, request);
            return "invoice/agency-monthly-bill-form";
        }
    }

    @GetMapping("/{billId}")
    public String view(@PathVariable Long billId, Model model) {
        model.addAttribute("bill", billService.getBill(billId));
        return "invoice/agency-monthly-bill-detail";
    }

    @PostMapping("/{billId}/delete")
    public String delete(
            @PathVariable Long billId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            RedirectAttributes redirectAttributes) {
        try {
            billService.softDelete(billId, resolveActorEmail());
            redirectAttributes.addFlashAttribute("successMessage", "Agency monthly bill deleted successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        redirectAttributes.addAttribute("page", Math.max(page, 0));
        redirectAttributes.addAttribute("size", Math.min(Math.max(size, 1), 100));
        return "redirect:/auditor/agency-monthly-bills";
    }

    private void populateForm(Model model, AgencyMonthlyBillGenerateRequest request) {
        model.addAttribute("billRequest", request);
        model.addAttribute("agencies", agencyMasterRepository.findByStatusOrderByAgencyNameAsc(AgencyStatus.ACTIVE));
        model.addAttribute("months", IntStream.rangeClosed(1, 12).boxed().toList());
        model.addAttribute("employeeTypes", AgencyMonthlyBillEmployeeType.values());
        int currentYear = YearMonth.now().getYear();
        List<Integer> years = IntStream.rangeClosed(currentYear - 3, currentYear + 1)
                .boxed()
                .sorted((first, second) -> Integer.compare(second, first))
                .toList();
        model.addAttribute("years", years);
    }

    private String resolveActorEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "SYSTEM";
        }
        return authentication.getName();
    }
}
