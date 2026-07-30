package com.maharecruitment.gov.in.web.controller.admin;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Base64;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.attendance.entity.HolidayMasterEntity;
import com.maharecruitment.gov.in.attendance.entity.WeekOffWorkingDayEntity;
import com.maharecruitment.gov.in.attendance.service.HolidayService;
import com.maharecruitment.gov.in.attendance.service.WeekOffWorkingDayService;
import com.maharecruitment.gov.in.audit.dto.AuditEventView;
import com.maharecruitment.gov.in.web.dto.FileUploadResult;
import com.maharecruitment.gov.in.web.dto.admin.AdminHolidayCalendarDayView;
import com.maharecruitment.gov.in.web.dto.admin.AdminHolidayForm;
import com.maharecruitment.gov.in.web.dto.admin.AdminWeekOffWorkingDayForm;
import com.maharecruitment.gov.in.web.dto.admin.AdminWeekOffWorkingDayView;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/attendance/holidays")
public class AdminHolidayCalendarPageController {

    private static final String WORKING_DAY_UPLOAD_MODULE = "attendance/week-off-working-day-office-order";

    private final HolidayService holidayService;
    private final WeekOffWorkingDayService weekOffWorkingDayService;
    private final FileStorageService fileStorageService;

    public AdminHolidayCalendarPageController(
            HolidayService holidayService,
            WeekOffWorkingDayService weekOffWorkingDayService,
            FileStorageService fileStorageService) {
        this.holidayService = holidayService;
        this.weekOffWorkingDayService = weekOffWorkingDayService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public String view(
            @RequestParam(name = "month", required = false) String monthValue,
            @RequestParam(name = "holidayId", required = false) Long holidayId,
            @RequestParam(name = "workingDayId", required = false) Long workingDayId,
            @RequestParam(name = "date", required = false) String holidayDateValue,
            @RequestParam(name = "workingDate", required = false) String workingDateValue,
            Model model) {
        YearMonth selectedMonth = resolveMonth(monthValue);
        populateViewModel(
                model,
                selectedMonth,
                holidayId,
                workingDayId,
                buildHolidayForm(holidayId, selectedMonth, resolveDate(holidayDateValue)),
                buildWorkingDayForm(workingDayId, selectedMonth, resolveDate(workingDateValue)),
                true,
                true);
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
            populateViewModel(
                    model,
                    selectedMonth,
                    holidayForm.getId(),
                    null,
                    holidayForm,
                    buildWorkingDayForm(null, selectedMonth, null),
                    true,
                    false);
            return "admin/attendance/holidays";
        }

        HolidayMasterEntity holiday = new HolidayMasterEntity();
        holiday.setId(holidayForm.getId());
        holiday.setHolidayDate(holidayForm.getHolidayDate());
        holiday.setHolidayName(holidayForm.getHolidayName());

        try {
            holidayService.saveHoliday(holiday);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    holidayForm.getId() == null
                            ? "Holiday added. Internal attendance will reflect it immediately."
                            : "Holiday updated. Internal attendance will reflect the change immediately.");
            return "redirect:/admin/attendance/holidays?month=" + selectedMonth;
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateViewModel(
                    model,
                    selectedMonth,
                    holidayForm.getId(),
                    null,
                    holidayForm,
                    buildWorkingDayForm(null, selectedMonth, null),
                    true,
                    false);
            return "admin/attendance/holidays";
        }
    }

    @PostMapping("/working-days")
    public String saveWorkingDay(
            @Valid @ModelAttribute("workingDayForm") AdminWeekOffWorkingDayForm workingDayForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        YearMonth selectedMonth = resolveMonth(workingDayForm.getViewMonth());
        String previousOfficeOrderOriginalName = workingDayForm.getOfficeOrderOriginalName();
        String previousOfficeOrderStoredName = workingDayForm.getOfficeOrderStoredName();
        String previousOfficeOrderPath = workingDayForm.getOfficeOrderPath();
        String previousOfficeOrderContentType = workingDayForm.getOfficeOrderContentType();
        Long previousOfficeOrderFileSize = workingDayForm.getOfficeOrderFileSize();
        validateWorkingDayForm(workingDayForm, bindingResult);

        FileUploadResult uploadedOfficeOrder = bindingResult.hasErrors()
                ? null
                : stageOfficeOrderUpload(workingDayForm, bindingResult);
        if (bindingResult.hasErrors()) {
            if (uploadedOfficeOrder != null) {
                fileStorageService.deleteQuietly(uploadedOfficeOrder.fullPath());
            }
            restoreOfficeOrderReference(
                    workingDayForm,
                    previousOfficeOrderOriginalName,
                    previousOfficeOrderStoredName,
                    previousOfficeOrderPath,
                    previousOfficeOrderContentType,
                    previousOfficeOrderFileSize);
            populateViewModel(
                    model,
                    selectedMonth,
                    null,
                    workingDayForm.getId(),
                    buildHolidayForm(null, selectedMonth, null),
                    workingDayForm,
                    false,
                    true);
            return "admin/attendance/holidays";
        }
        WeekOffWorkingDayEntity workingDay = new WeekOffWorkingDayEntity();
        workingDay.setId(workingDayForm.getId());
        workingDay.setWorkingDate(workingDayForm.getWorkingDate());
        workingDay.setOfficeOrderOriginalName(workingDayForm.getOfficeOrderOriginalName());
        workingDay.setOfficeOrderStoredName(workingDayForm.getOfficeOrderStoredName());
        workingDay.setOfficeOrderPath(workingDayForm.getOfficeOrderPath());
        workingDay.setOfficeOrderContentType(workingDayForm.getOfficeOrderContentType());
        workingDay.setOfficeOrderFileSize(workingDayForm.getOfficeOrderFileSize());

        try {
            weekOffWorkingDayService.saveWorkingDay(workingDay);
            if (uploadedOfficeOrder != null
                    && StringUtils.hasText(previousOfficeOrderPath)
                    && !previousOfficeOrderPath.equals(uploadedOfficeOrder.fullPath())
                    && fileStorageService.isManagedPath(previousOfficeOrderPath)) {
                fileStorageService.deleteQuietly(previousOfficeOrderPath);
            }
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    workingDayForm.getId() == null
                            ? "Week off working day added. Internal attendance will now treat the weekend as a working day."
                            : "Week off working day updated. Internal attendance will use the latest office order immediately.");
            return "redirect:/admin/attendance/holidays?month=" + selectedMonth;
        } catch (RuntimeException ex) {
            if (uploadedOfficeOrder != null) {
                fileStorageService.deleteQuietly(uploadedOfficeOrder.fullPath());
            }
            restoreOfficeOrderReference(
                    workingDayForm,
                    previousOfficeOrderOriginalName,
                    previousOfficeOrderStoredName,
                    previousOfficeOrderPath,
                    previousOfficeOrderContentType,
                    previousOfficeOrderFileSize);
            model.addAttribute("errorMessage", ex.getMessage());
            populateViewModel(
                    model,
                    selectedMonth,
                    null,
                    workingDayForm.getId(),
                    buildHolidayForm(null, selectedMonth, null),
                    workingDayForm,
                    false,
                    true);
            return "admin/attendance/holidays";
        }
    }

    private void restoreOfficeOrderReference(
            AdminWeekOffWorkingDayForm workingDayForm,
            String officeOrderOriginalName,
            String officeOrderStoredName,
            String officeOrderPath,
            String officeOrderContentType,
            Long officeOrderFileSize) {
        workingDayForm.setOfficeOrderOriginalName(officeOrderOriginalName);
        workingDayForm.setOfficeOrderStoredName(officeOrderStoredName);
        workingDayForm.setOfficeOrderPath(officeOrderPath);
        workingDayForm.setOfficeOrderContentType(officeOrderContentType);
        workingDayForm.setOfficeOrderFileSize(officeOrderFileSize);
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

    @PostMapping("/working-days/{workingDayId}/archive")
    public String archiveWorkingDay(
            @PathVariable Long workingDayId,
            @RequestParam(name = "month", required = false) String monthValue,
            RedirectAttributes redirectAttributes) {
        YearMonth selectedMonth = resolveMonth(monthValue);
        try {
            weekOffWorkingDayService.archiveWorkingDay(workingDayId);
            redirectAttributes.addFlashAttribute("successMessage", "Working-day override archived successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/attendance/holidays?month=" + selectedMonth;
    }

    private void populateViewModel(
            Model model,
            YearMonth selectedMonth,
            Long holidayId,
            Long workingDayId,
            AdminHolidayForm holidayForm,
            AdminWeekOffWorkingDayForm workingDayForm,
            boolean preserveHolidayForm,
            boolean preserveWorkingDayForm) {
        LocalDate monthStart = selectedMonth.atDay(1);
        LocalDate monthEnd = selectedMonth.atEndOfMonth();
        List<HolidayMasterEntity> monthHolidays = holidayService.getHolidaysBetween(monthStart, monthEnd);
        List<WeekOffWorkingDayEntity> monthWorkingDays = weekOffWorkingDayService.getWorkingDaysBetween(monthStart, monthEnd);

        Map<LocalDate, HolidayMasterEntity> holidayByDate = new LinkedHashMap<>();
        for (HolidayMasterEntity holiday : monthHolidays) {
            if (holiday.getHolidayDate() != null) {
                holidayByDate.put(holiday.getHolidayDate(), holiday);
            }
        }

        Map<LocalDate, WeekOffWorkingDayEntity> workingDayByDate = new LinkedHashMap<>();
        for (WeekOffWorkingDayEntity workingDay : monthWorkingDays) {
            if (workingDay.getWorkingDate() != null) {
                workingDayByDate.put(workingDay.getWorkingDate(), workingDay);
            }
        }

        HolidayMasterEntity selectedHoliday = holidayId == null ? null : holidayService.getHolidayById(holidayId);
        WeekOffWorkingDayEntity selectedWorkingDay = workingDayId == null
                ? null
                : weekOffWorkingDayService.getWorkingDayById(workingDayId);
        List<AuditEventView> activityTimeline = selectedHoliday == null
                ? List.of()
                : holidayService.getHolidayAuditTrail(selectedHoliday.getId());
        List<AuditEventView> workingDayActivityTimeline = selectedWorkingDay == null
                ? List.of()
                : weekOffWorkingDayService.getWorkingDayAuditTrail(selectedWorkingDay.getId());

        if (!preserveHolidayForm) {
            holidayForm = buildHolidayForm(holidayId, selectedMonth, null);
        }
        if (!preserveWorkingDayForm) {
            workingDayForm = buildWorkingDayForm(workingDayId, selectedMonth, null);
        }
        holidayForm.setViewMonth(selectedMonth.toString());
        workingDayForm.setViewMonth(selectedMonth.toString());

        long workingDayHolidayCount = monthHolidays.stream()
                .filter(holiday -> isWorkingDayHoliday(holiday.getHolidayDate()))
                .count();

        model.addAttribute("holidayForm", holidayForm);
        model.addAttribute("workingDayForm", workingDayForm);
        model.addAttribute("pageTitle", "Holiday Calendar");
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("selectedMonthValue", selectedMonth.toString());
        model.addAttribute(
                "selectedMonthLabel",
                selectedMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + selectedMonth.getYear());
        model.addAttribute("monthHolidays", monthHolidays);
        model.addAttribute("monthWorkingDays", buildWorkingDayViews(monthWorkingDays));
        model.addAttribute("calendarWeeks", buildCalendarWeeks(selectedMonth, holidayByDate, workingDayByDate));
        model.addAttribute("selectedHoliday", selectedHoliday);
        model.addAttribute("activityTimeline", activityTimeline);
        model.addAttribute("selectedWorkingDay", selectedWorkingDay);
        model.addAttribute("workingDayActivityTimeline", workingDayActivityTimeline);
        model.addAttribute(
                "selectedWorkingDayDocumentPathToken",
                selectedWorkingDay == null ? null : encodeDocumentPath(selectedWorkingDay.getOfficeOrderPath()));
        model.addAttribute("monthlyHolidayCount", monthHolidays.size());
        model.addAttribute("workingDayHolidayCount", workingDayHolidayCount);
        model.addAttribute("weekendHolidayCount", monthHolidays.size() - workingDayHolidayCount);
        model.addAttribute("monthlyWorkingDayOverrideCount", monthWorkingDays.size());
        model.addAttribute("today", LocalDate.now());
    }

    private AdminHolidayForm buildHolidayForm(Long holidayId, YearMonth selectedMonth, LocalDate selectedDate) {
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
        } else if (selectedDate != null) {
            form.setHolidayDate(selectedDate);
        }

        return form;
    }

    private AdminWeekOffWorkingDayForm buildWorkingDayForm(
            Long workingDayId,
            YearMonth selectedMonth,
            LocalDate selectedDate) {
        AdminWeekOffWorkingDayForm form = new AdminWeekOffWorkingDayForm();
        form.setViewMonth(selectedMonth.toString());

        WeekOffWorkingDayEntity workingDay = workingDayId == null ? null : weekOffWorkingDayService.getWorkingDayById(workingDayId);
        if (workingDay != null) {
            form.setId(workingDay.getId());
            form.setWorkingDate(workingDay.getWorkingDate());
            form.setOfficeOrderOriginalName(workingDay.getOfficeOrderOriginalName());
            form.setOfficeOrderStoredName(workingDay.getOfficeOrderStoredName());
            form.setOfficeOrderPath(workingDay.getOfficeOrderPath());
            form.setOfficeOrderContentType(workingDay.getOfficeOrderContentType());
            form.setOfficeOrderFileSize(workingDay.getOfficeOrderFileSize());
            if (workingDay.getWorkingDate() != null) {
                form.setViewMonth(YearMonth.from(workingDay.getWorkingDate()).toString());
            }
        } else if (selectedDate != null) {
            form.setWorkingDate(selectedDate);
        }

        return form;
    }

    private List<AdminWeekOffWorkingDayView> buildWorkingDayViews(List<WeekOffWorkingDayEntity> monthWorkingDays) {
        return monthWorkingDays.stream()
                .map(workingDay -> new AdminWeekOffWorkingDayView(
                        workingDay.getId(),
                        workingDay.getWorkingDate(),
                        workingDay.getOfficeOrderOriginalName(),
                        encodeDocumentPath(workingDay.getOfficeOrderPath())))
                .toList();
    }

    private List<List<AdminHolidayCalendarDayView>> buildCalendarWeeks(
            YearMonth selectedMonth,
            Map<LocalDate, HolidayMasterEntity> holidayByDate,
            Map<LocalDate, WeekOffWorkingDayEntity> workingDayByDate) {
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
                WeekOffWorkingDayEntity workingDay = workingDayByDate.get(cursor);
                boolean weekend = isWeekend(cursor);
                week.add(new AdminHolidayCalendarDayView(
                        cursor,
                        YearMonth.from(cursor).equals(selectedMonth),
                        cursor.equals(today),
                        holiday != null,
                        weekend,
                        holiday != null && !weekend,
                        holiday != null ? holiday.getHolidayName() : null,
                        workingDay != null,
                        workingDay != null ? workingDay.getId() : null));
                cursor = cursor.plusDays(1);
            }
            weeks.add(week);
        }
        return weeks;
    }

    private FileUploadResult stageOfficeOrderUpload(
            AdminWeekOffWorkingDayForm workingDayForm,
            BindingResult bindingResult) {
        MultipartFile officeOrderFile = workingDayForm.getOfficeOrderFile();
        if (!hasFile(officeOrderFile)) {
            return null;
        }

        try {
            FileUploadResult uploadedFile = fileStorageService.store(officeOrderFile, WORKING_DAY_UPLOAD_MODULE);
            workingDayForm.setOfficeOrderOriginalName(uploadedFile.originalFileName());
            workingDayForm.setOfficeOrderStoredName(uploadedFile.storedFileName());
            workingDayForm.setOfficeOrderPath(uploadedFile.fullPath());
            workingDayForm.setOfficeOrderContentType(uploadedFile.contentType());
            workingDayForm.setOfficeOrderFileSize(uploadedFile.size());
            return uploadedFile;
        } catch (RuntimeException ex) {
            bindingResult.rejectValue(
                    "officeOrderFile",
                    "workingDay.officeOrderFile",
                    "Office order upload failed: " + resolveMessage(ex));
            return null;
        }
    }

    private void validateWorkingDayForm(
            AdminWeekOffWorkingDayForm workingDayForm,
            BindingResult bindingResult) {
        if (workingDayForm.getWorkingDate() != null && !isWeekend(workingDayForm.getWorkingDate())) {
            bindingResult.rejectValue(
                    "workingDate",
                    "workingDay.workingDate",
                    "Only Saturday or Sunday can be converted into a working day.");
        }

        if (!hasFile(workingDayForm.getOfficeOrderFile())) {
            if (!StringUtils.hasText(workingDayForm.getOfficeOrderPath())) {
                bindingResult.rejectValue(
                        "officeOrderFile",
                        "workingDay.officeOrderFile",
                        "Office order is required.");
                return;
            }

            validateStoredOfficeOrderReference(workingDayForm.getOfficeOrderPath(), bindingResult);
        }
    }

    private void validateStoredOfficeOrderReference(String officeOrderPath, BindingResult bindingResult) {
        if (!fileStorageService.isManagedFileAllowed(officeOrderPath, WORKING_DAY_UPLOAD_MODULE)) {
            bindingResult.rejectValue(
                    "officeOrderFile",
                    "workingDay.officeOrderFile",
                    "Existing office order reference is invalid.");
        }
    }

    private String encodeDocumentPath(String fullPath) {
        if (!StringUtils.hasText(fullPath)) {
            return null;
        }
        return Base64.getEncoder().encodeToString(fullPath.getBytes(StandardCharsets.UTF_8));
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

    private LocalDate resolveDate(String dateValue) {
        if (StringUtils.hasText(dateValue)) {
            try {
                return LocalDate.parse(dateValue.trim());
            } catch (RuntimeException ignored) {
                // Ignore invalid date and keep the add form blank.
            }
        }
        return null;
    }

    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private String resolveMessage(RuntimeException ex) {
        return ex.getMessage() == null ? "Upload failed." : ex.getMessage();
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private boolean isWorkingDayHoliday(LocalDate date) {
        return date != null && !isWeekend(date);
    }
}
