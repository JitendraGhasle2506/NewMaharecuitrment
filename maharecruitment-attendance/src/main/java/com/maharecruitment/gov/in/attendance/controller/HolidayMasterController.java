package com.maharecruitment.gov.in.attendance.controller;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
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

import com.maharecruitment.gov.in.attendance.dto.CommonCalendarDayView;
import com.maharecruitment.gov.in.attendance.dto.CalendarDateMode;
import com.maharecruitment.gov.in.attendance.dto.CommonCalendarEntryForm;
import com.maharecruitment.gov.in.attendance.dto.CommonCalendarItemView;
import com.maharecruitment.gov.in.attendance.dto.CommonCalendarPhotoView;
import com.maharecruitment.gov.in.attendance.dto.CommonWorkingDayView;
import com.maharecruitment.gov.in.attendance.dto.WeekOffWorkingDayForm;
import com.maharecruitment.gov.in.attendance.entity.CalendarEntryType;
import com.maharecruitment.gov.in.attendance.entity.CommonCalendarEntryEntity;
import com.maharecruitment.gov.in.attendance.entity.CommonCalendarEntryPhotoEntity;
import com.maharecruitment.gov.in.attendance.entity.HolidayMasterEntity;
import com.maharecruitment.gov.in.attendance.entity.WeekOffWorkingDayEntity;
import com.maharecruitment.gov.in.attendance.service.CommonCalendarEntryService;
import com.maharecruitment.gov.in.attendance.service.HolidayService;
import com.maharecruitment.gov.in.attendance.service.WeekOffWorkingDayService;
import com.maharecruitment.gov.in.audit.dto.AuditEventView;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/common/holidays")
public class HolidayMasterController {

    private static final String CALENDAR_VIEW = "attendance/holiday-list";
    private static final String REDIRECT = "redirect:/common/holidays";
    private static final String HOLIDAY_SOURCE = "HOLIDAY";
    private static final String COMMON_SOURCE = "COMMON";

    private final HolidayService holidayService;
    private final CommonCalendarEntryService calendarEntryService;
    private final WeekOffWorkingDayService workingDayService;

    public HolidayMasterController(
            HolidayService holidayService,
            CommonCalendarEntryService calendarEntryService,
            WeekOffWorkingDayService workingDayService) {
        this.holidayService = holidayService;
        this.calendarEntryService = calendarEntryService;
        this.workingDayService = workingDayService;
    }

    @GetMapping({"", "/"})
    public String view(
            @RequestParam(name = "month", required = false) String monthValue,
            @RequestParam(name = "holidayId", required = false) Long holidayId,
            @RequestParam(name = "calendarEntryId", required = false) Long calendarEntryId,
            @RequestParam(name = "workingDayId", required = false) Long workingDayId,
            @RequestParam(name = "date", required = false) String dateValue,
            @RequestParam(name = "workingDate", required = false) String workingDateValue,
            @RequestParam(name = "type", required = false) CalendarEntryType entryType,
            Model model) {
        YearMonth selectedMonth = resolveMonth(monthValue);
        CommonCalendarEntryForm form = model.containsAttribute("calendarForm")
                ? (CommonCalendarEntryForm) model.getAttribute("calendarForm")
                : buildForm(holidayId, calendarEntryId, selectedMonth, resolveDate(dateValue), entryType);
        populateModel(
                model,
                selectedMonth,
                form,
                holidayId,
                calendarEntryId,
                workingDayId,
                resolveDate(workingDateValue),
                entryType);
        return CALENDAR_VIEW;
    }

    @GetMapping("/add")
    public String legacyAdd() {
        return REDIRECT;
    }

    @GetMapping("/edit/{id}")
    public String legacyEdit(@PathVariable Long id) {
        return REDIRECT + "?holidayId=" + id;
    }

    @PostMapping("/save")
    public String save(
            @Valid @ModelAttribute("calendarForm") CommonCalendarEntryForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        YearMonth selectedMonth = resolveMonth(form.getViewMonth());
        validateSource(form, bindingResult);
        if (bindingResult.hasErrors()) {
            populateModel(model, selectedMonth, form, selectedHolidayId(form), selectedCommonEntryId(form), null, null,
                    form.getEntryType());
            return CALENDAR_VIEW;
        }

        try {
            boolean editing = form.getId() != null;
            if (form.getEntryType() == CalendarEntryType.HOLIDAY) {
                HolidayMasterEntity holiday = new HolidayMasterEntity();
                holiday.setId(form.getId());
                holiday.setHolidayDate(form.getEntryDate());
                holiday.setHolidayName(form.getTitle());
                holidayService.saveHoliday(holiday);
            } else {
                CommonCalendarEntryEntity entry = new CommonCalendarEntryEntity();
                entry.setId(form.getId());
                entry.setEntryDate(form.getEntryDate());
                entry.setEndDate(form.getDateMode() == CalendarDateMode.RANGE ? form.getEndDate() : null);
                entry.setTitle(form.getTitle());
                entry.setEntryType(form.getEntryType());
                entry.setDescription(form.getDescription());
                calendarEntryService.saveEntry(entry);
            }
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    form.getEntryType().getDisplayName() + (editing ? " updated successfully." : " added successfully."));
            return REDIRECT + "?month=" + YearMonth.from(form.getEntryDate());
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", resolveMessage(ex));
            populateModel(model, selectedMonth, form, selectedHolidayId(form), selectedCommonEntryId(form), null, null,
                    form.getEntryType());
            return CALENDAR_VIEW;
        }
    }

    @PostMapping("/holidays/{id}/archive")
    public String archiveHoliday(
            @PathVariable Long id,
            @RequestParam(name = "month", required = false) String monthValue,
            RedirectAttributes redirectAttributes) {
        try {
            holidayService.archiveHoliday(id);
            redirectAttributes.addFlashAttribute("successMessage", "Holiday archived successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", resolveMessage(ex));
        }
        return REDIRECT + "?month=" + resolveMonth(monthValue);
    }

    @PostMapping("/entries/{id}/archive")
    public String archiveEntry(
            @PathVariable Long id,
            @RequestParam(name = "month", required = false) String monthValue,
            RedirectAttributes redirectAttributes) {
        try {
            calendarEntryService.archiveEntry(id);
            redirectAttributes.addFlashAttribute("successMessage", "Calendar entry archived successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", resolveMessage(ex));
        }
        return REDIRECT + "?month=" + resolveMonth(monthValue);
    }

    private void populateModel(
            Model model,
            YearMonth selectedMonth,
            CommonCalendarEntryForm form,
            Long selectedHolidayId,
            Long selectedCalendarEntryId,
            Long selectedWorkingDayId,
            LocalDate selectedWorkingDate,
            CalendarEntryType requestedType) {
        LocalDate monthStart = selectedMonth.atDay(1);
        LocalDate monthEnd = selectedMonth.atEndOfMonth();
        List<CommonCalendarItemView> items = new ArrayList<>();
        holidayService.getHolidaysBetween(monthStart, monthEnd).stream().map(this::toItem).forEach(items::add);
        List<CommonCalendarEntryEntity> calendarEntries = calendarEntryService.getEntriesBetween(monthStart, monthEnd);
        calendarEntries.stream().map(this::toItem).forEach(items::add);
        List<WeekOffWorkingDayEntity> workingDays = workingDayService.getWorkingDaysBetween(monthStart, monthEnd);
        Map<Long, List<CommonCalendarPhotoView>> photosByEntryId = buildPhotoViews(calendarEntries);
        items.sort(Comparator.comparing(CommonCalendarItemView::date)
                .thenComparing(item -> item.entryType().ordinal())
                .thenComparing(CommonCalendarItemView::title, String.CASE_INSENSITIVE_ORDER));

        Map<LocalDate, List<CommonCalendarItemView>> itemsByDate = new LinkedHashMap<>();
        items.forEach(item -> addItemToDates(itemsByDate, item, monthStart, monthEnd));
        Map<LocalDate, WeekOffWorkingDayEntity> workingDaysByDate = new LinkedHashMap<>();
        workingDays.forEach(workingDay -> workingDaysByDate.put(workingDay.getWorkingDate(), workingDay));

        List<AuditEventView> activityTimeline = List.of();
        CommonCalendarItemView selectedItem = null;
        if (selectedHolidayId != null) {
            selectedItem = itemFromForm(form);
            activityTimeline = holidayService.getHolidayAuditTrail(selectedHolidayId);
        } else if (selectedCalendarEntryId != null) {
            selectedItem = itemFromForm(form);
            activityTimeline = calendarEntryService.getEntryAuditTrail(selectedCalendarEntryId);
        }

        WeekOffWorkingDayEntity selectedWorkingDay = findWorkingDay(workingDays, selectedWorkingDayId);
        WeekOffWorkingDayForm workingDayForm = model.containsAttribute("workingDayForm")
                ? (WeekOffWorkingDayForm) model.getAttribute("workingDayForm")
                : buildWorkingDayForm(selectedWorkingDay, selectedMonth, selectedWorkingDate, requestedType);

        model.addAttribute("calendarForm", form);
        model.addAttribute("entryTypes", CalendarEntryType.values());
        model.addAttribute("selectedMonthValue", selectedMonth.toString());
        model.addAttribute("selectedMonthLabel",
                selectedMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + selectedMonth.getYear());
        model.addAttribute("previousMonth", selectedMonth.minusMonths(1).toString());
        model.addAttribute("nextMonth", selectedMonth.plusMonths(1).toString());
        model.addAttribute("calendarWeeks", buildCalendarWeeks(selectedMonth, itemsByDate, workingDaysByDate));
        model.addAttribute("monthItems", items);
        model.addAttribute("photosByEntryId", photosByEntryId);
        model.addAttribute("selectedPhotos", selectedCalendarEntryId == null
                ? List.of()
                : photosByEntryId.getOrDefault(selectedCalendarEntryId, List.of()));
        model.addAttribute("monthWorkingDays", workingDays.stream().map(this::toWorkingDayView).toList());
        model.addAttribute("workingDayForm", workingDayForm);
        model.addAttribute("selectedWorkingDayDocumentPathToken",
                selectedWorkingDay == null ? null : encodeDocumentPath(selectedWorkingDay.getOfficeOrderPath()));
        model.addAttribute("monthlyHolidayCount", countType(items, CalendarEntryType.HOLIDAY));
        model.addAttribute("monthlyFestivalCount", countType(items, CalendarEntryType.FESTIVAL));
        model.addAttribute("monthlyEventCount", countType(items, CalendarEntryType.EVENT));
        model.addAttribute("monthlyWorkingDayCount", workingDays.size());
        model.addAttribute("selectedItem", selectedItem);
        model.addAttribute("activityTimeline", activityTimeline);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("pageTitle", "Holiday, Festival, Event & Working Day Calendar");
    }

    private CommonCalendarEntryForm buildForm(
            Long holidayId,
            Long calendarEntryId,
            YearMonth selectedMonth,
            LocalDate selectedDate,
            CalendarEntryType requestedType) {
        CommonCalendarEntryForm form = new CommonCalendarEntryForm();
        form.setViewMonth(selectedMonth.toString());
        form.setEntryType(requestedType == null ? CalendarEntryType.HOLIDAY : requestedType);
        form.setEntryDate(selectedDate);

        if (holidayId != null) {
            HolidayMasterEntity holiday = holidayService.getHolidayById(holidayId);
            if (holiday != null) {
                form.setId(holiday.getId());
                form.setSource(HOLIDAY_SOURCE);
                form.setEntryType(CalendarEntryType.HOLIDAY);
                form.setEntryDate(holiday.getHolidayDate());
                form.setTitle(holiday.getHolidayName());
            }
        } else if (calendarEntryId != null) {
            CommonCalendarEntryEntity entry = calendarEntryService.getEntryById(calendarEntryId);
            if (entry != null) {
                form.setId(entry.getId());
                form.setSource(COMMON_SOURCE);
                form.setEntryType(entry.getEntryType());
                form.setEntryDate(entry.getEntryDate());
                form.setEndDate(entry.getEndDate());
                form.setDateMode(entry.getEndDate() == null ? CalendarDateMode.SINGLE : CalendarDateMode.RANGE);
                form.setTitle(entry.getTitle());
                form.setDescription(entry.getDescription());
            }
        }
        return form;
    }

    private void validateSource(CommonCalendarEntryForm form, BindingResult bindingResult) {
        if (form.getId() == null) {
            form.setSource(form.getEntryType() == CalendarEntryType.HOLIDAY ? HOLIDAY_SOURCE : COMMON_SOURCE);
            return;
        }
        if (HOLIDAY_SOURCE.equals(form.getSource()) && form.getEntryType() != CalendarEntryType.HOLIDAY) {
            bindingResult.rejectValue("entryType", "calendar.type", "An existing holiday cannot be changed into a festival or event.");
        } else if (COMMON_SOURCE.equals(form.getSource()) && form.getEntryType() == CalendarEntryType.HOLIDAY) {
            bindingResult.rejectValue("entryType", "calendar.type", "An existing festival or event cannot be changed into an attendance holiday.");
        } else if (!HOLIDAY_SOURCE.equals(form.getSource()) && !COMMON_SOURCE.equals(form.getSource())) {
            bindingResult.reject("calendar.source", "The calendar entry source is invalid.");
        }
    }

    private List<List<CommonCalendarDayView>> buildCalendarWeeks(
            YearMonth selectedMonth,
            Map<LocalDate, List<CommonCalendarItemView>> itemsByDate,
            Map<LocalDate, WeekOffWorkingDayEntity> workingDaysByDate) {
        List<List<CommonCalendarDayView>> weeks = new ArrayList<>();
        LocalDate cursor = selectedMonth.atDay(1);
        while (cursor.getDayOfWeek() != DayOfWeek.MONDAY) cursor = cursor.minusDays(1);
        LocalDate gridEnd = selectedMonth.atEndOfMonth();
        while (gridEnd.getDayOfWeek() != DayOfWeek.SUNDAY) gridEnd = gridEnd.plusDays(1);
        LocalDate today = LocalDate.now();
        while (!cursor.isAfter(gridEnd)) {
            List<CommonCalendarDayView> week = new ArrayList<>(7);
            for (int index = 0; index < 7; index++) {
                WeekOffWorkingDayEntity workingDay = workingDaysByDate.get(cursor);
                List<CommonCalendarItemView> dayItems = List.copyOf(itemsByDate.getOrDefault(cursor, List.of()));
                week.add(new CommonCalendarDayView(
                        cursor,
                        YearMonth.from(cursor).equals(selectedMonth),
                        cursor.equals(today),
                        isWeekend(cursor),
                        dayItems.stream().anyMatch(CommonCalendarItemView::isHoliday),
                        workingDay != null,
                        workingDay == null ? null : workingDay.getId(),
                        dayItems));
                cursor = cursor.plusDays(1);
            }
            weeks.add(week);
        }
        return weeks;
    }

    private CommonCalendarItemView toItem(HolidayMasterEntity holiday) {
        return new CommonCalendarItemView(
                holiday.getId(), HOLIDAY_SOURCE, holiday.getHolidayDate(), null, holiday.getHolidayName(),
                CalendarEntryType.HOLIDAY, "Affects internal attendance.");
    }

    private CommonCalendarItemView toItem(CommonCalendarEntryEntity entry) {
        return new CommonCalendarItemView(
                entry.getId(), COMMON_SOURCE, entry.getEntryDate(), entry.getEndDate(), entry.getTitle(),
                entry.getEntryType(), entry.getDescription());
    }

    private CommonCalendarItemView itemFromForm(CommonCalendarEntryForm form) {
        return new CommonCalendarItemView(
                form.getId(), form.getSource(), form.getEntryDate(),
                form.getDateMode() == CalendarDateMode.RANGE ? form.getEndDate() : null, form.getTitle(),
                form.getEntryType(), form.getDescription());
    }

    private void addItemToDates(
            Map<LocalDate, List<CommonCalendarItemView>> itemsByDate,
            CommonCalendarItemView item,
            LocalDate monthStart,
            LocalDate monthEnd) {
        LocalDate cursor = item.date().isBefore(monthStart) ? monthStart : item.date();
        LocalDate lastDate = item.effectiveEndDate().isAfter(monthEnd) ? monthEnd : item.effectiveEndDate();
        while (!cursor.isAfter(lastDate)) {
            itemsByDate.computeIfAbsent(cursor, ignored -> new ArrayList<>()).add(item);
            cursor = cursor.plusDays(1);
        }
    }

    private WeekOffWorkingDayEntity findWorkingDay(List<WeekOffWorkingDayEntity> workingDays, Long selectedId) {
        if (selectedId == null) {
            return null;
        }
        return workingDays.stream()
                .filter(workingDay -> selectedId.equals(workingDay.getId()))
                .findFirst()
                .orElseGet(() -> workingDayService.getWorkingDayById(selectedId));
    }

    private WeekOffWorkingDayForm buildWorkingDayForm(
            WeekOffWorkingDayEntity workingDay,
            YearMonth selectedMonth,
            LocalDate selectedDate,
            CalendarEntryType requestedType) {
        WeekOffWorkingDayForm form = new WeekOffWorkingDayForm();
        form.setViewMonth(selectedMonth.toString());
        form.setReturnType(requestedType == null ? null : requestedType.name());
        form.setWorkingDate(selectedDate);
        if (workingDay != null) {
            form.setId(workingDay.getId());
            form.setWorkingDate(workingDay.getWorkingDate());
            form.setOfficeOrderOriginalName(workingDay.getOfficeOrderOriginalName());
            form.setOfficeOrderStoredName(workingDay.getOfficeOrderStoredName());
            form.setOfficeOrderPath(workingDay.getOfficeOrderPath());
            form.setOfficeOrderContentType(workingDay.getOfficeOrderContentType());
            form.setOfficeOrderFileSize(workingDay.getOfficeOrderFileSize());
        }
        return form;
    }

    private CommonWorkingDayView toWorkingDayView(WeekOffWorkingDayEntity workingDay) {
        return new CommonWorkingDayView(
                workingDay.getId(),
                workingDay.getWorkingDate(),
                workingDay.getOfficeOrderOriginalName(),
                encodeDocumentPath(workingDay.getOfficeOrderPath()));
    }

    private Map<Long, List<CommonCalendarPhotoView>> buildPhotoViews(
            List<CommonCalendarEntryEntity> calendarEntries) {
        List<Long> entryIds = calendarEntries.stream().map(CommonCalendarEntryEntity::getId).toList();
        Map<Long, List<CommonCalendarPhotoView>> photosByEntryId = new LinkedHashMap<>();
        for (CommonCalendarEntryPhotoEntity photo : calendarEntryService.getPhotosForEntries(entryIds)) {
            photosByEntryId.computeIfAbsent(photo.getCalendarEntryId(), ignored -> new ArrayList<>())
                    .add(new CommonCalendarPhotoView(
                            photo.getId(),
                            photo.getCalendarEntryId(),
                            photo.getOriginalName(),
                            encodeDocumentPath(photo.getFilePath())));
        }
        photosByEntryId.replaceAll((ignored, photos) -> List.copyOf(photos));
        return photosByEntryId;
    }

    private String encodeDocumentPath(String fullPath) {
        if (!StringUtils.hasText(fullPath)) {
            return null;
        }
        return Base64.getEncoder().encodeToString(fullPath.getBytes(StandardCharsets.UTF_8));
    }

    private long countType(List<CommonCalendarItemView> items, CalendarEntryType type) {
        return items.stream().filter(item -> item.entryType() == type).count();
    }

    private Long selectedHolidayId(CommonCalendarEntryForm form) {
        return HOLIDAY_SOURCE.equals(form.getSource()) ? form.getId() : null;
    }

    private Long selectedCommonEntryId(CommonCalendarEntryForm form) {
        return COMMON_SOURCE.equals(form.getSource()) ? form.getId() : null;
    }

    private YearMonth resolveMonth(String value) {
        if (StringUtils.hasText(value)) {
            try {
                return YearMonth.parse(value.trim());
            } catch (RuntimeException ignored) {
                // Use current month.
            }
        }
        return YearMonth.now();
    }

    private LocalDate resolveDate(String value) {
        if (StringUtils.hasText(value)) {
            try {
                return LocalDate.parse(value.trim());
            } catch (RuntimeException ignored) {
                // Leave date empty.
            }
        }
        return null;
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private String resolveMessage(RuntimeException ex) {
        return StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "Unable to update the calendar.";
    }
}
