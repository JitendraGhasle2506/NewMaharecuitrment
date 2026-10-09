package com.maharecruitment.gov.in.web.service.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import com.maharecruitment.gov.in.attendance.dto.WeekOffWorkingDayForm;
import com.maharecruitment.gov.in.attendance.entity.WeekOffWorkingDayEntity;
import com.maharecruitment.gov.in.attendance.service.WeekOffWorkingDayService;
import com.maharecruitment.gov.in.web.dto.FileUploadResult;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;

class WeekOffWorkingDayPageServiceTest {

    private final WeekOffWorkingDayService workingDayService = mock(WeekOffWorkingDayService.class);
    private final FileStorageService fileStorageService = mock(FileStorageService.class);
    private final WeekOffWorkingDayPageService service =
            new WeekOffWorkingDayPageService(workingDayService, fileStorageService);

    @Test
    void createsWeekendWorkingDayWithOfficeOrder() {
        WeekOffWorkingDayForm form = form(LocalDate.of(2026, 10, 10));
        form.setOfficeOrderFile(new MockMultipartFile(
                "officeOrderFile", "order.pdf", "application/pdf", "%PDF-1.4".getBytes()));
        when(fileStorageService.store(any(), any())).thenReturn(
                new FileUploadResult("order.pdf", "stored.pdf", "C:/uploads/stored.pdf", "application/pdf", 8));

        assertThat(service.save(form)).isTrue();

        ArgumentCaptor<WeekOffWorkingDayEntity> entity = ArgumentCaptor.forClass(WeekOffWorkingDayEntity.class);
        verify(workingDayService).saveWorkingDay(entity.capture());
        assertThat(entity.getValue().getWorkingDate()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(entity.getValue().getOfficeOrderPath()).isEqualTo("C:/uploads/stored.pdf");
    }

    @Test
    void editUsesTrustedExistingOfficeOrderWhenNoReplacementIsUploaded() {
        WeekOffWorkingDayEntity existing = new WeekOffWorkingDayEntity();
        existing.setId(15L);
        existing.setOfficeOrderOriginalName("trusted.pdf");
        existing.setOfficeOrderStoredName("trusted-stored.pdf");
        existing.setOfficeOrderPath("C:/uploads/trusted.pdf");
        existing.setOfficeOrderContentType("application/pdf");
        existing.setOfficeOrderFileSize(100L);
        when(workingDayService.getWorkingDayById(15L)).thenReturn(existing);

        WeekOffWorkingDayForm form = form(LocalDate.of(2026, 10, 11));
        form.setId(15L);
        form.setOfficeOrderPath("C:/tampered/path.pdf");

        assertThat(service.save(form)).isFalse();

        ArgumentCaptor<WeekOffWorkingDayEntity> entity = ArgumentCaptor.forClass(WeekOffWorkingDayEntity.class);
        verify(workingDayService).saveWorkingDay(entity.capture());
        assertThat(entity.getValue().getOfficeOrderPath()).isEqualTo("C:/uploads/trusted.pdf");
        verify(fileStorageService, never()).store(any(), any());
    }

    @Test
    void rejectsWeekdayBeforeUploading() {
        WeekOffWorkingDayForm form = form(LocalDate.of(2026, 10, 12));

        assertThatThrownBy(() -> service.save(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Saturday or Sunday");
        verify(fileStorageService, never()).store(any(), any());
        verify(workingDayService, never()).saveWorkingDay(any());
    }

    private WeekOffWorkingDayForm form(LocalDate date) {
        WeekOffWorkingDayForm form = new WeekOffWorkingDayForm();
        form.setWorkingDate(date);
        return form;
    }
}
