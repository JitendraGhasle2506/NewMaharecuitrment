package com.maharecruitment.gov.in.attendance.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.maharecruitment.gov.in.attendance.dto.CommonCalendarEntryForm;
import com.maharecruitment.gov.in.attendance.entity.CalendarEntryType;
import com.maharecruitment.gov.in.attendance.entity.CommonCalendarEntryEntity;
import com.maharecruitment.gov.in.attendance.entity.HolidayMasterEntity;
import com.maharecruitment.gov.in.attendance.service.CommonCalendarEntryService;
import com.maharecruitment.gov.in.attendance.service.HolidayService;
import com.maharecruitment.gov.in.attendance.service.WeekOffWorkingDayService;

class HolidayMasterControllerTest {

    private final HolidayService holidayService = mock(HolidayService.class);
    private final CommonCalendarEntryService calendarEntryService = mock(CommonCalendarEntryService.class);
    private final WeekOffWorkingDayService workingDayService = mock(WeekOffWorkingDayService.class);
    private final HolidayMasterController controller =
            new HolidayMasterController(holidayService, calendarEntryService, workingDayService);

    @Test
    void festivalUsesInformationCalendarService() {
        CommonCalendarEntryForm form = validForm(CalendarEntryType.FESTIVAL);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "calendarForm");

        String view = controller.save(
                form, bindingResult, new ConcurrentModel(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/common/holidays?month=2026-10");
        verify(calendarEntryService).saveEntry(any(CommonCalendarEntryEntity.class));
        verify(holidayService, never()).saveHoliday(any());
    }

    @Test
    void holidayUsesAttendanceHolidayService() {
        CommonCalendarEntryForm form = validForm(CalendarEntryType.HOLIDAY);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "calendarForm");

        controller.save(form, bindingResult, new ConcurrentModel(), new RedirectAttributesModelMap());

        verify(holidayService).saveHoliday(any(HolidayMasterEntity.class));
        verify(calendarEntryService, never()).saveEntry(any());
    }

    @Test
    void selectedFestivalDoesNotLoadAuditTimelineForCalendarScreen() {
        CommonCalendarEntryEntity festival = new CommonCalendarEntryEntity();
        festival.setId(31L);
        festival.setEntryDate(LocalDate.of(2026, 10, 11));
        festival.setTitle("Navratri");
        festival.setEntryType(CalendarEntryType.FESTIVAL);
        when(calendarEntryService.getEntryById(31L)).thenReturn(festival);
        when(calendarEntryService.getEntriesBetween(any(), any())).thenReturn(List.of(festival));
        when(holidayService.getHolidaysBetween(any(), any())).thenReturn(List.of());
        when(workingDayService.getWorkingDaysBetween(any(), any())).thenReturn(List.of());

        ConcurrentModel model = new ConcurrentModel();
        String view = controller.view("2026-10", null, 31L, null, null, null,
                CalendarEntryType.FESTIVAL, model);

        assertThat(view).isEqualTo("attendance/holiday-list");
        assertThat(model).doesNotContainKey("activityTimeline");
        verify(calendarEntryService, never()).getEntryAuditTrail(any());
    }

    private CommonCalendarEntryForm validForm(CalendarEntryType type) {
        CommonCalendarEntryForm form = new CommonCalendarEntryForm();
        form.setViewMonth("2026-10");
        form.setEntryDate(LocalDate.of(2026, 10, 20));
        form.setEntryType(type);
        form.setTitle("Calendar entry");
        return form;
    }
}
