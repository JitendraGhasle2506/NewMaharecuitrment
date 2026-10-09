package com.maharecruitment.gov.in.web.controller.common;

import java.time.YearMonth;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import com.maharecruitment.gov.in.attendance.dto.WeekOffWorkingDayForm;
import com.maharecruitment.gov.in.attendance.entity.CalendarEntryType;
import com.maharecruitment.gov.in.attendance.service.WeekOffWorkingDayService;
import com.maharecruitment.gov.in.web.service.attendance.WeekOffWorkingDayPageService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/common/holidays/working-days")
public class CommonWorkingDayController {

    private static final String FORM_ATTRIBUTE = "workingDayForm";
    private static final String BINDING_RESULT_ATTRIBUTE =
            BindingResult.MODEL_KEY_PREFIX + FORM_ATTRIBUTE;

    private final WeekOffWorkingDayPageService workingDayPageService;
    private final WeekOffWorkingDayService workingDayService;

    public CommonWorkingDayController(
            WeekOffWorkingDayPageService workingDayPageService,
            WeekOffWorkingDayService workingDayService) {
        this.workingDayPageService = workingDayPageService;
        this.workingDayService = workingDayService;
    }

    @PostMapping
    public String save(
            @Valid @ModelAttribute(FORM_ATTRIBUTE) WeekOffWorkingDayForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        YearMonth month = resolveMonth(form.getViewMonth());
        if (bindingResult.hasErrors()) {
            form.setOfficeOrderFile(null);
            redirectAttributes.addFlashAttribute(BINDING_RESULT_ATTRIBUTE, bindingResult);
            redirectAttributes.addFlashAttribute(FORM_ATTRIBUTE, form);
            return redirect(month, form.getReturnType(), form.getId());
        }

        try {
            boolean created = workingDayPageService.save(form);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    created
                            ? "Weekend working day added successfully. Internal attendance is updated immediately."
                            : "Weekend working day updated successfully.");
        } catch (RuntimeException ex) {
            form.setOfficeOrderFile(null);
            redirectAttributes.addFlashAttribute(FORM_ATTRIBUTE, form);
            redirectAttributes.addFlashAttribute("errorMessage", resolveMessage(ex));
            return redirect(month, form.getReturnType(), form.getId());
        }
        return redirect(month, form.getReturnType(), null);
    }

    @PostMapping("/{id}/archive")
    public String archive(
            @PathVariable Long id,
            @RequestParam(name = "month", required = false) String monthValue,
            @RequestParam(name = "type", required = false) String returnType,
            RedirectAttributes redirectAttributes) {
        try {
            workingDayService.archiveWorkingDay(id);
            redirectAttributes.addFlashAttribute("successMessage", "Working-day override archived successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", resolveMessage(ex));
        }
        return redirect(resolveMonth(monthValue), returnType, null);
    }

    private String redirect(YearMonth month, String type, Long workingDayId) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/common/holidays")
                .queryParam("month", month);
        String safeType = resolveType(type);
        if (safeType != null) {
            uri.queryParam("type", safeType);
        }
        if (workingDayId != null) {
            uri.queryParam("workingDayId", workingDayId);
        }
        return "redirect:" + uri.build().toUriString();
    }

    private String resolveType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return CalendarEntryType.valueOf(value.trim().toUpperCase()).name();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private YearMonth resolveMonth(String value) {
        if (StringUtils.hasText(value)) {
            try {
                return YearMonth.parse(value.trim());
            } catch (RuntimeException ignored) {
                // Use the current month.
            }
        }
        return YearMonth.now();
    }

    private String resolveMessage(RuntimeException ex) {
        return StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "Unable to save the working day.";
    }
}
