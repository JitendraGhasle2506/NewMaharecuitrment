package com.maharecruitment.gov.in.web.controller.admin;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.attendance.entity.HolidayMasterEntity;
import com.maharecruitment.gov.in.attendance.service.HolidayService;
import com.maharecruitment.gov.in.audit.dto.AuditEventView;
import com.maharecruitment.gov.in.web.dto.admin.AdminHolidayCalendarDayView;
import com.maharecruitment.gov.in.web.dto.admin.AdminHolidayForm;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/attendance/holidays")
public class AdminHolidayCalendarPageController {

    private final HolidayService holidayService;

    public AdminHolidayCalendarPageController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    @GetMapping
    public String view(
            @RequestParam(name = "month", required = false) String monthValue,
            @RequestParam(name = "holidayId", required = false) Long holidayId,
            Model model) {
        YearMonth selectedMonth = resolveMonth(monthValue);
        populateViewModel(model, selectedMonth, holidayId, buildForm(holidayId, selectedMonth), false);
        return "admin/attendance/holidays";
    }

    @PostMapping
    public String save(
            @Valid @ModelAttribute("holidayForm") AdminHolidayForm holidayForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        YearMonth selectedMonth = resolveMonth(holidayForm.getViewMonth());
        if (bindingResult.hasErrors()) {
            populateViewModel(model, selectedMonth, holidayForm.getId(), holidayForm, true);
            return "admin/attendance/holidays";
        }

        HolidayMasterEntity holiday = new HolidayMasterEntity();
        holiday.setId(holidayForm.getId());
        holiday.setHolidayDate(holidayForm.getHolidayDate());
        holiday.setHolidayName(holidayForm.getHolidayName());

        try {
            HolidayMasterEntity savedHoliday = holidayService.saveHoliday(holiday);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    holidayForm.getId() == null
                            ? "Holiday added. Internal attendance will reflect it immediately."
                            : "Holiday updated. Internal attendance will reflect the change immediately.");
            return "redirect:/admin/attendance/holidays?month=" + selectedMonth + "&holidayId=" + savedHoliday.getId();
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateViewModel(model, selectedMonth, holidayForm.getId(), holidayForm, true);
            return "admin/attendance/holidays";
        }
    }

    @PostMapping("/{holidayId}/archive")
    public String archive(
            @PathVariable Long holidayId,
            @RequestParam(name = "month", required = false) String monthValue,
            RedirectAttributes redirectAttributes) {
        YearMonth selectedMonth = resolveMonth(monthValue);
        try {
            holidayService.archiveHoliday(holidayId);
            redirectAttributes.addFlashAttribute("successMessage", "Holiday archived successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/attendance/holidays?month=" + selectedMonth;
    }

    private void populateViewModel(
            Model model,
            YearMonth selectedMonth,
            Long holidayId,
            AdminHolidayForm holidayForm,
            boolean preserveForm) {
        LocalDate monthStart = selectedMonth.atDay(1);
        LocalDate monthEnd = selectedMonth.atEndOfMonth();
        List<HolidayMasterEntity> monthHolidays = holidayService.getHolidaysBetween(monthStart, monthEnd);
        Map<LocalDate, HolidayMasterEntity> holidayByDate = new LinkedHashMap<>();
        for (HolidayMasterEntity holiday : monthHolidays) {
            if (holiday.getHolidayDate() != null) {
                holidayByDate.put(holiday.getHolidayDate(), holiday);
            }
        }

        HolidayMasterEntity selectedHoliday = holidayId == null ? null : holidayService.getHolidayById(holidayId);
        List<AuditEventView> activityTimeline = selectedHoliday == null
                ? List.of()
                : holidayService.getHolidayAuditTrail(selectedHoliday.getId());

        if (!preserveForm) {
            holidayForm = buildForm(holidayId, selectedMonth);
        }
        holidayForm.setViewMonth(selectedMonth.toString());

        long workingDayHolidayCount = monthHolidays.stream()
                .filter(holiday -> isWorkingDayHoliday(holiday.getHolidayDate()))
                .count();

        model.addAttribute("holidayForm", holidayForm);
        model.addAttribute("pageTitle", "Holiday Calendar");
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("selectedMonthValue", selectedMonth.toString());
        model.addAttribute(
                "selectedMonthLabel",
                selectedMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + selectedMonth.getYear());
        model.addAttribute("monthHolidays", monthHolidays);
        model.addAttribute("calendarWeeks", buildCalendarWeeks(selectedMonth, holidayByDate));
        model.addAttribute("selectedHoliday", selectedHoliday);
        model.addAttribute("activityTimeline", activityTimeline);
        model.addAttribute("monthlyHolidayCount", monthHolidays.size());
        model.addAttribute("workingDayHolidayCount", workingDayHolidayCount);
        model.addAttribute("weekendHolidayCount", monthHolidays.size() - workingDayHolidayCount);
        model.addAttribute("today", LocalDate.now());
    }

    private AdminHolidayForm buildForm(Long holidayId, YearMonth selectedMonth) {
        AdminHolidayForm form = new AdminHolidayForm();
        form.setViewMonth(selectedMonth.toString());

        HolidayMasterEntity holiday = holidayId == null ? null : holidayService.getHolidayById(holidayId);
        if (holiday != null) {
            form.setId(holiday.getId());
            form.setHolidayDate(holiday.getHolidayDate());
            form.setHolidayName(holiday.getHolidayName());
            if (holiday.getHolidayDate() != null) {
                form.setViewMonth(YearMonth.from(holiday.getHolidayDate()).toString());
            }
        } else {
            form.setHolidayDate(selectedMonth.atDay(1));
        }

        return form;
    }

    private List<List<AdminHolidayCalendarDayView>> buildCalendarWeeks(
            YearMonth selectedMonth,
            Map<LocalDate, HolidayMasterEntity> holidayByDate) {
        List<List<AdminHolidayCalendarDayView>> weeks = new ArrayList<>();
        LocalDate firstDay = selectedMonth.atDay(1);
        LocalDate lastDay = selectedMonth.atEndOfMonth();
        LocalDate cursor = firstDay;
        while (cursor.getDayOfWeek() != DayOfWeek.MONDAY) {
            cursor = cursor.minusDays(1);
        }

        LocalDate gridEnd = lastDay;
        while (gridEnd.getDayOfWeek() != DayOfWeek.SUNDAY) {
            gridEnd = gridEnd.plusDays(1);
        }

        LocalDate today = LocalDate.now();
        while (!cursor.isAfter(gridEnd)) {
            List<AdminHolidayCalendarDayView> week = new ArrayList<>();
            for (int index = 0; index < 7; index++) {
                HolidayMasterEntity holiday = holidayByDate.get(cursor);
                boolean weekend = isWeekend(cursor);
                week.add(new AdminHolidayCalendarDayView(
                        cursor,
                        YearMonth.from(cursor).equals(selectedMonth),
                        cursor.equals(today),
                        holiday != null,
                        weekend,
                        holiday != null && !weekend,
                        holiday != null ? holiday.getHolidayName() : null));
                cursor = cursor.plusDays(1);
            }
            weeks.add(week);
        }
        return weeks;
    }

    private YearMonth resolveMonth(String monthValue) {
        if (StringUtils.hasText(monthValue)) {
            try {
                return YearMonth.parse(monthValue.trim());
            } catch (RuntimeException ignored) {
                // Fallback to the current month below.
            }
        }
        return YearMonth.now();
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private boolean isWorkingDayHoliday(LocalDate date) {
        return date != null && !isWeekend(date);
    }
}
