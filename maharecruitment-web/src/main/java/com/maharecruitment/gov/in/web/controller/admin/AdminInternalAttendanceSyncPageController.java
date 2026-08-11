package com.maharecruitment.gov.in.web.controller.admin;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.attendance.config.InternalAttendanceSyncProperties;
import com.maharecruitment.gov.in.attendance.service.InternalAttendanceSyncService;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceSyncResult;
import com.maharecruitment.gov.in.web.dto.admin.InternalAttendanceSyncForm;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/attendance/internal-sync")
public class AdminInternalAttendanceSyncPageController {

    private static final DateTimeFormatter UPSTREAM_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final InternalAttendanceSyncService internalAttendanceSyncService;
    private final InternalAttendanceSyncProperties internalAttendanceSyncProperties;

    public AdminInternalAttendanceSyncPageController(
            InternalAttendanceSyncService internalAttendanceSyncService,
            InternalAttendanceSyncProperties internalAttendanceSyncProperties) {
        this.internalAttendanceSyncService = internalAttendanceSyncService;
        this.internalAttendanceSyncProperties = internalAttendanceSyncProperties;
    }

    @GetMapping
    public String view(Model model) {
        if (!model.containsAttribute("syncForm")) {
            model.addAttribute("syncForm", buildDefaultForm());
        }
        populateViewModel(model);
        return "admin/attendance/internal-sync";
    }

    @PostMapping
    public String runManualSync(
            @Valid @ModelAttribute("syncForm") InternalAttendanceSyncForm syncForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        validateDateRange(syncForm, bindingResult);
        if (bindingResult.hasErrors()) {
            populateViewModel(model);
            return "admin/attendance/internal-sync";
        }

        try {
            InternalAttendanceSyncResult result = internalAttendanceSyncService.syncAttendance(
                    syncForm.getStartDate(),
                    syncForm.getEndDate());
            if (result.getFailureMessage() != null) {
                redirectAttributes.addFlashAttribute(
                        "warningMessage",
                        result.getFailureMessage());
            } else if (result.getEmployeesFailed() > 0) {
                redirectAttributes.addFlashAttribute(
                        "warningMessage",
                        "Internal attendance sync finished with employee-level failures. Review application logs for failed employee codes.");
            } else {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Internal attendance sync completed for the requested date range.");
            }
            redirectAttributes.addFlashAttribute("syncForm", syncForm);
            redirectAttributes.addFlashAttribute("syncResult", result);
            return "redirect:/admin/attendance/internal-sync";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateViewModel(model);
            return "admin/attendance/internal-sync";
        }
    }

    private void populateViewModel(Model model) {
        model.addAttribute("maxSelectableDate", resolveToday());
        model.addAttribute("syncEnabled", internalAttendanceSyncProperties.isEnabled());
        model.addAttribute("syncApiUrl", buildUpstreamApiExample(resolveToday(), resolveToday()));
        model.addAttribute("syncCron", internalAttendanceSyncProperties.getSchedulerCron());
        model.addAttribute("syncZone", internalAttendanceSyncProperties.getSchedulerZone());
        model.addAttribute("currentDateOnly", internalAttendanceSyncProperties.isCurrentDateOnly());
        model.addAttribute("eligibleEmployeeCount", internalAttendanceSyncService.countEligibleInternalEmployees());
        model.addAttribute(
                "uniqueCodeRule",
                internalAttendanceSyncProperties.getUniqueCodePrefix() + " + employee Aadhaar last 4 digits");
        model.addAttribute(
                "manualApiExample",
                "/attendance/internal-sync/run-now?startDate=2026-05-01&endDate=2026-05-31");
    }

    private InternalAttendanceSyncForm buildDefaultForm() {
        LocalDate today = resolveToday();

        InternalAttendanceSyncForm form = new InternalAttendanceSyncForm();
        form.setStartDate(YearMonth.from(today).atDay(1));
        form.setEndDate(today);
        return form;
    }

    private void validateDateRange(InternalAttendanceSyncForm syncForm, BindingResult bindingResult) {
        if (syncForm.getStartDate() == null || syncForm.getEndDate() == null) {
            return;
        }

        LocalDate today = resolveToday();
        if (syncForm.getStartDate().isAfter(today)) {
            bindingResult.rejectValue(
                    "startDate",
                    "attendance.sync.startDate.future",
                    "Start date cannot be a future date.");
        }
        if (syncForm.getEndDate().isAfter(today)) {
            bindingResult.rejectValue(
                    "endDate",
                    "attendance.sync.endDate.future",
                    "End date cannot be a future date.");
        }
        if (syncForm.getEndDate().isBefore(syncForm.getStartDate())) {
            bindingResult.rejectValue(
                    "endDate",
                    "attendance.sync.endDate",
                    "End date must be on or after the start date.");
        }
    }

    private LocalDate resolveToday() {
        ZoneId zoneId = ZoneId.of(internalAttendanceSyncProperties.getSchedulerZone());
        return LocalDate.now(zoneId);
    }

    private String buildUpstreamApiExample(LocalDate startDate, LocalDate endDate) {
        return internalAttendanceSyncProperties.reportApiUrl()
                + "?start_date="
                + UPSTREAM_DATE_FORMAT.format(startDate)
                + "&end_date="
                + UPSTREAM_DATE_FORMAT.format(endDate);
    }
}
