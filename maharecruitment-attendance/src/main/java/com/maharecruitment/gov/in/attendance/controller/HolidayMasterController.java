package com.maharecruitment.gov.in.attendance.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.attendance.entity.HolidayMasterEntity;
import com.maharecruitment.gov.in.attendance.service.HolidayService;

@Controller
@RequestMapping("/common/holidays")
public class HolidayMasterController {

    private static final String HOLIDAY_LIST_VIEW = "attendance/holiday-list";
    private static final String HOLIDAY_FORM_VIEW = "attendance/holiday-form";
    private static final String HOLIDAY_LIST_REDIRECT = "redirect:/common/holidays";

    @Autowired
    private HolidayService holidayService;

    @GetMapping({ "", "/" })
    public String listHolidays(
            @RequestParam(value = "holidayId", required = false) Long holidayId,
            Model model) {
        List<HolidayMasterEntity> holidays = holidayService.getAllHolidays();
        populateListModel(model, holidays, holidayId);
        model.addAttribute("pageTitle", "Holiday Master");
        return HOLIDAY_LIST_VIEW;
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        populateFormModel(model, new HolidayMasterEntity(), "Add Holiday");
        return HOLIDAY_FORM_VIEW;
    }

    @PostMapping("/save")
    public String saveHoliday(
            @ModelAttribute HolidayMasterEntity holiday,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            HolidayMasterEntity savedHoliday = holidayService.saveHoliday(holiday);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    holiday.getId() == null ? "Holiday saved successfully." : "Holiday updated successfully.");
            return HOLIDAY_LIST_REDIRECT + "?holidayId=" + savedHoliday.getId();
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormModel(model, holiday, holiday.getId() == null ? "Add Holiday" : "Edit Holiday");
            return HOLIDAY_FORM_VIEW;
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        HolidayMasterEntity holiday = holidayService.getHolidayById(id);
        if (holiday == null) {
            return HOLIDAY_LIST_REDIRECT;
        }
        populateFormModel(model, holiday, "Edit Holiday");
        return HOLIDAY_FORM_VIEW;
    }

    @GetMapping("/archive/{id}")
    public String archiveHoliday(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            holidayService.archiveHoliday(id);
            redirectAttributes.addFlashAttribute("successMessage", "Holiday archived successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return HOLIDAY_LIST_REDIRECT;
    }

    private void populateListModel(Model model, List<HolidayMasterEntity> holidays, Long selectedHolidayId) {
        LocalDate today = LocalDate.now();
        HolidayMasterEntity selectedHoliday = selectedHolidayId == null
                ? null
                : holidayService.getHolidayById(selectedHolidayId);
        HolidayMasterEntity nextHoliday = holidays.stream()
                .filter(holiday -> holiday.getHolidayDate() != null && !holiday.getHolidayDate().isBefore(today))
                .findFirst()
                .orElse(null);

        long currentYearHolidayCount = holidays.stream()
                .filter(holiday -> holiday.getHolidayDate() != null && holiday.getHolidayDate().getYear() == today.getYear())
                .count();
        long upcomingHolidayCount = holidays.stream()
                .filter(holiday -> holiday.getHolidayDate() != null && !holiday.getHolidayDate().isBefore(today))
                .count();
        long weekendHolidayCount = holidays.stream()
                .filter(holiday -> isWeekend(holiday.getHolidayDate()))
                .count();

        model.addAttribute("holidays", holidays);
        model.addAttribute("selectedHoliday", selectedHoliday);
        model.addAttribute(
                "activityTimeline",
                selectedHoliday == null ? List.of() : holidayService.getHolidayAuditTrail(selectedHoliday.getId()));
        model.addAttribute("totalHolidayCount", holidays.size());
        model.addAttribute("currentYearHolidayCount", currentYearHolidayCount);
        model.addAttribute("upcomingHolidayCount", upcomingHolidayCount);
        model.addAttribute("weekendHolidayCount", weekendHolidayCount);
        model.addAttribute("nextHoliday", nextHoliday);
        model.addAttribute(
                "daysUntilNextHoliday",
                nextHoliday == null ? null : ChronoUnit.DAYS.between(today, nextHoliday.getHolidayDate()));
        model.addAttribute("today", today);
    }

    private void populateFormModel(Model model, HolidayMasterEntity holiday, String pageTitle) {
        LocalDate today = LocalDate.now();
        List<HolidayMasterEntity> allHolidays = holidayService.getAllHolidays().stream()
                .filter(item -> item.getHolidayDate() != null)
                .toList();
        List<HolidayMasterEntity> previewHolidays = allHolidays.stream()
                .filter(item -> !item.getHolidayDate().isBefore(today))
                .limit(5)
                .toList();
        if (previewHolidays.isEmpty()) {
            int previewOffset = Math.max(allHolidays.size() - 5, 0);
            previewHolidays = allHolidays.stream()
                    .skip(previewOffset)
                    .toList();
        }

        model.addAttribute("holiday", holiday);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("isEditMode", holiday.getId() != null);
        model.addAttribute("previewHolidays", previewHolidays);
        model.addAttribute("holidayDayLabel", buildHolidayDayLabel(holiday.getHolidayDate()));
        model.addAttribute("holidayCount", allHolidays.size());
    }

    private String buildHolidayDayLabel(LocalDate holidayDate) {
        if (holidayDate == null) {
            return "Date not selected";
        }
        return holidayDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + ", " + holidayDate;
    }

    private boolean isWeekend(LocalDate holidayDate) {
        if (holidayDate == null) {
            return false;
        }
        DayOfWeek dayOfWeek = holidayDate.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}
