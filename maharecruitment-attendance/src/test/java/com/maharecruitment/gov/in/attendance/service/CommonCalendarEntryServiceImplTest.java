package com.maharecruitment.gov.in.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.attendance.entity.CalendarEntryType;
import com.maharecruitment.gov.in.attendance.dto.CommonCalendarPhotoUpload;
import com.maharecruitment.gov.in.attendance.entity.CommonCalendarEntryEntity;
import com.maharecruitment.gov.in.attendance.entity.CommonCalendarEntryPhotoEntity;
import com.maharecruitment.gov.in.attendance.repository.CommonCalendarEntryRepository;
import com.maharecruitment.gov.in.attendance.repository.CommonCalendarEntryPhotoRepository;
import com.maharecruitment.gov.in.audit.dto.AuditRecordRequest;
import com.maharecruitment.gov.in.audit.service.AuditTrailService;
import com.maharecruitment.gov.in.common.service.CurrentActorProvider;

@ExtendWith(MockitoExtension.class)
class CommonCalendarEntryServiceImplTest {

    @Mock private CommonCalendarEntryRepository repository;
    @Mock private CommonCalendarEntryPhotoRepository photoRepository;
    @Mock private AuditTrailService auditTrailService;
    @Mock private CurrentActorProvider currentActorProvider;

    private CommonCalendarEntryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CommonCalendarEntryServiceImpl(
                repository, photoRepository, auditTrailService, currentActorProvider);
    }

    @Test
    void savesFestivalWithoutCreatingAttendanceHoliday() {
        CommonCalendarEntryEntity festival = entry(null, CalendarEntryType.FESTIVAL, "Diwali Celebration");
        when(repository.existsDuplicate(festival.getEntryDate(), festival.getEntryDate(),
                festival.getEntryType(), festival.getTitle(), null))
                .thenReturn(false);
        when(repository.save(any(CommonCalendarEntryEntity.class))).thenAnswer(invocation -> {
            CommonCalendarEntryEntity saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        CommonCalendarEntryEntity saved = service.saveEntry(festival);

        assertThat(saved.getId()).isEqualTo(11L);
        assertThat(saved.getEntryType()).isEqualTo(CalendarEntryType.FESTIVAL);
        ArgumentCaptor<AuditRecordRequest> audit = ArgumentCaptor.forClass(AuditRecordRequest.class);
        verify(auditTrailService).record(audit.capture());
        assertThat(audit.getValue().getModuleName()).isEqualTo("COMMON");
        assertThat(audit.getValue().getActionType()).isEqualTo("CREATE");
    }

    @Test
    void rejectsHolidayTypeBecauseAttendanceHolidaysUseHolidayService() {
        CommonCalendarEntryEntity holiday = entry(null, CalendarEntryType.HOLIDAY, "Public Holiday");

        assertThatThrownBy(() -> service.saveEntry(holiday))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Festival or Event");
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsDuplicateFestivalOnSameDate() {
        CommonCalendarEntryEntity festival = entry(null, CalendarEntryType.FESTIVAL, "Foundation Festival");
        when(repository.existsDuplicate(festival.getEntryDate(), festival.getEntryDate(),
                festival.getEntryType(), festival.getTitle(), null))
                .thenReturn(true);

        assertThatThrownBy(() -> service.saveEntry(festival))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already configured");
    }

    @Test
    void archivesEventAndRecordsAudit() {
        CommonCalendarEntryEntity event = entry(12L, CalendarEntryType.EVENT, "Town Hall");
        when(repository.findByIdAndActiveTrue(12L)).thenReturn(Optional.of(event));
        when(repository.save(event)).thenReturn(event);

        service.archiveEntry(12L);

        assertThat(event.getActive()).isFalse();
        verify(auditTrailService).record(any(AuditRecordRequest.class));
    }

    @Test
    void savesAllUploadedPhotosAgainstTheCreatedEvent() {
        CommonCalendarEntryEntity event = entry(null, CalendarEntryType.EVENT, "Town Hall");
        when(repository.existsDuplicate(event.getEntryDate(), event.getEntryDate(),
                event.getEntryType(), event.getTitle(), null))
                .thenReturn(false);
        when(repository.save(any(CommonCalendarEntryEntity.class))).thenAnswer(invocation -> {
            CommonCalendarEntryEntity saved = invocation.getArgument(0);
            saved.setId(25L);
            return saved;
        });

        service.saveEntryWithPhotos(event, List.of(
                new CommonCalendarPhotoUpload("one.jpg", "stored-one.jpg", "C:/one.jpg", "image/jpeg", 10),
                new CommonCalendarPhotoUpload("two.png", "stored-two.png", "C:/two.png", "image/png", 20)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CommonCalendarEntryPhotoEntity>> photos = ArgumentCaptor.forClass(List.class);
        verify(photoRepository).saveAll(photos.capture());
        assertThat(photos.getValue()).hasSize(2).allSatisfy(photo ->
                assertThat(photo.getCalendarEntryId()).isEqualTo(25L));
    }

    private CommonCalendarEntryEntity entry(Long id, CalendarEntryType type, String title) {
        CommonCalendarEntryEntity entry = new CommonCalendarEntryEntity();
        entry.setId(id);
        entry.setEntryDate(LocalDate.of(2026, 10, 20));
        entry.setEntryType(type);
        entry.setTitle(title);
        entry.setDescription("Organization calendar information");
        entry.setActive(Boolean.TRUE);
        return entry;
    }
}
