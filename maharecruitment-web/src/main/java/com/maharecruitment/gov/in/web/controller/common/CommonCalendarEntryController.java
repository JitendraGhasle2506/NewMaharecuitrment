package com.maharecruitment.gov.in.web.controller.common;

import java.time.YearMonth;
import java.util.ArrayList;

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

import com.maharecruitment.gov.in.attendance.dto.CommonCalendarEntryForm;
import com.maharecruitment.gov.in.attendance.entity.CalendarEntryType;
import com.maharecruitment.gov.in.web.service.attendance.CommonCalendarEntryPageService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/common/holidays/calendar-entries")
public class CommonCalendarEntryController {

    private static final String FORM_ATTRIBUTE = "calendarForm";
    private static final String BINDING_RESULT_ATTRIBUTE =
            BindingResult.MODEL_KEY_PREFIX + FORM_ATTRIBUTE;

    private final CommonCalendarEntryPageService pageService;

    public CommonCalendarEntryController(CommonCalendarEntryPageService pageService) {
        this.pageService = pageService;
    }

    @PostMapping
    public String save(
            @Valid @ModelAttribute(FORM_ATTRIBUTE) CommonCalendarEntryForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        YearMonth month = resolveMonth(form);
        if (bindingResult.hasErrors()) {
            preserveForm(form, bindingResult, redirectAttributes);
            return redirect(month, form, true);
        }
        try {
            boolean created = pageService.save(form);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    form.getEntryType().getDisplayName() + (created ? " added successfully." : " updated successfully."));
            return redirect(YearMonth.from(form.getEntryDate()), form, false);
        } catch (RuntimeException ex) {
            form.setPhotos(new ArrayList<>());
            redirectAttributes.addFlashAttribute(FORM_ATTRIBUTE, form);
            redirectAttributes.addFlashAttribute("errorMessage", resolveMessage(ex));
            return redirect(month, form, true);
        }
    }

    @PostMapping("/{entryId}/photos/{photoId}/archive")
    public String archivePhoto(
            @PathVariable Long entryId,
            @PathVariable Long photoId,
            @RequestParam(name = "month", required = false) String monthValue,
            @RequestParam(name = "type", required = false) CalendarEntryType type,
            RedirectAttributes redirectAttributes) {
        try {
            pageService.removePhoto(entryId, photoId);
            redirectAttributes.addFlashAttribute("successMessage", "Photo removed successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", resolveMessage(ex));
        }
        CommonCalendarEntryForm form = new CommonCalendarEntryForm();
        form.setId(entryId);
        form.setEntryType(type == null ? CalendarEntryType.EVENT : type);
        form.setSource("COMMON");
        return redirect(resolveMonth(monthValue), form, true);
    }

    private void preserveForm(
            CommonCalendarEntryForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        form.setPhotos(new ArrayList<>());
        redirectAttributes.addFlashAttribute(BINDING_RESULT_ATTRIBUTE, bindingResult);
        redirectAttributes.addFlashAttribute(FORM_ATTRIBUTE, form);
    }

    private String redirect(YearMonth month, CommonCalendarEntryForm form, boolean keepSelection) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/common/holidays")
                .queryParam("month", month);
        if (form.getEntryType() != null) {
            uri.queryParam("type", form.getEntryType().name());
        }
        if (keepSelection && form.getId() != null) {
            uri.queryParam("calendarEntryId", form.getId());
        }
        return "redirect:" + uri.build().toUriString();
    }

    private YearMonth resolveMonth(CommonCalendarEntryForm form) {
        if (form.getEntryDate() != null) {
            return YearMonth.from(form.getEntryDate());
        }
        return resolveMonth(form.getViewMonth());
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
        return StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "Unable to save the calendar entry.";
    }
}
