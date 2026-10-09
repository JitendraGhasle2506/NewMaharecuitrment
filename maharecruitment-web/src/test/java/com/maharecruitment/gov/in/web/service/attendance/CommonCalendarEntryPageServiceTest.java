package com.maharecruitment.gov.in.web.service.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import com.maharecruitment.gov.in.attendance.dto.CommonCalendarEntryForm;
import com.maharecruitment.gov.in.attendance.dto.CalendarDateMode;
import com.maharecruitment.gov.in.attendance.dto.CommonCalendarPhotoUpload;
import com.maharecruitment.gov.in.attendance.entity.CalendarEntryType;
import com.maharecruitment.gov.in.attendance.entity.CommonCalendarEntryEntity;
import com.maharecruitment.gov.in.attendance.service.CommonCalendarEntryService;
import com.maharecruitment.gov.in.attendance.service.HolidayService;
import com.maharecruitment.gov.in.web.dto.FileUploadResult;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;

class CommonCalendarEntryPageServiceTest {

    private final HolidayService holidayService = mock(HolidayService.class);
    private final CommonCalendarEntryService calendarEntryService = mock(CommonCalendarEntryService.class);
    private final FileStorageService fileStorageService = mock(FileStorageService.class);
    private final CommonCalendarEntryPageService service = new CommonCalendarEntryPageService(
            holidayService, calendarEntryService, fileStorageService);

    @Test
    void storesMultiplePhotosForOneEventInOneDatabaseOperation() {
        CommonCalendarEntryForm form = eventForm();
        form.setPhotos(List.of(photo("one.jpg"), photo("two.png")));
        when(fileStorageService.store(any(), anyString()))
                .thenReturn(upload("one.jpg", "C:/uploads/one.jpg"))
                .thenReturn(upload("two.png", "C:/uploads/two.png"));

        assertThat(service.save(form)).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CommonCalendarPhotoUpload>> photos = ArgumentCaptor.forClass(List.class);
        verify(calendarEntryService).saveEntryWithPhotos(any(CommonCalendarEntryEntity.class), photos.capture());
        assertThat(photos.getValue()).hasSize(2);
        verify(fileStorageService, times(2)).store(any(), anyString());
    }

    @Test
    void rejectsUploadsBeyondPerEventLimitBeforeWritingFiles() {
        CommonCalendarEntryForm form = eventForm();
        form.setId(22L);
        form.setSource("COMMON");
        form.setPhotos(List.of(photo("one.jpg"), photo("two.jpg")));
        when(calendarEntryService.countActivePhotos(22L)).thenReturn(9L);

        assertThatThrownBy(() -> service.save(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum of 10 photos");
        verify(fileStorageService, never()).store(any(), anyString());
    }

    @Test
    void removesStagedFilesWhenDatabaseSaveFails() {
        CommonCalendarEntryForm form = eventForm();
        form.setPhotos(List.of(photo("one.jpg"), photo("two.jpg")));
        when(fileStorageService.store(any(), anyString()))
                .thenReturn(upload("one.jpg", "C:/uploads/one.jpg"))
                .thenReturn(upload("two.jpg", "C:/uploads/two.jpg"));
        when(calendarEntryService.saveEntryWithPhotos(any(), anyList()))
                .thenThrow(new IllegalArgumentException("Duplicate event"));

        assertThatThrownBy(() -> service.save(form)).hasMessage("Duplicate event");
        verify(fileStorageService).deleteQuietly("C:/uploads/one.jpg");
        verify(fileStorageService).deleteQuietly("C:/uploads/two.jpg");
    }

    @Test
    void preventsPhotosOnAttendanceHoliday() {
        CommonCalendarEntryForm form = eventForm();
        form.setEntryType(CalendarEntryType.HOLIDAY);
        form.setPhotos(List.of(photo("holiday.jpg")));

        assertThatThrownBy(() -> service.save(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only to festivals and events");
        verify(holidayService, never()).saveHoliday(any());
    }

    @Test
    void savesFestivalDateRangeWithItsEndDate() {
        CommonCalendarEntryForm form = eventForm();
        form.setEntryType(CalendarEntryType.FESTIVAL);
        form.setDateMode(CalendarDateMode.RANGE);
        form.setEndDate(LocalDate.of(2026, 10, 24));

        service.save(form);

        ArgumentCaptor<CommonCalendarEntryEntity> entry =
                ArgumentCaptor.forClass(CommonCalendarEntryEntity.class);
        verify(calendarEntryService).saveEntryWithPhotos(entry.capture(), anyList());
        assertThat(entry.getValue().getEntryDate()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(entry.getValue().getEndDate()).isEqualTo(LocalDate.of(2026, 10, 24));
    }

    @Test
    void rejectsRangeWithoutToDate() {
        CommonCalendarEntryForm form = eventForm();
        form.setDateMode(CalendarDateMode.RANGE);

        assertThatThrownBy(() -> service.save(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("To date is required");
        verify(calendarEntryService, never()).saveEntryWithPhotos(any(), anyList());
    }

    private CommonCalendarEntryForm eventForm() {
        CommonCalendarEntryForm form = new CommonCalendarEntryForm();
        form.setEntryType(CalendarEntryType.EVENT);
        form.setEntryDate(LocalDate.of(2026, 10, 20));
        form.setTitle("Town hall");
        return form;
    }

    private MockMultipartFile photo(String name) {
        return new MockMultipartFile("photos", name, "image/jpeg", new byte[] { 1, 2, 3 });
    }

    private FileUploadResult upload(String name, String path) {
        return new FileUploadResult(name, "stored-" + name, path, "image/jpeg", 3);
    }
}
