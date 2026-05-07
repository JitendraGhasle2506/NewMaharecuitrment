package com.maharecruitment.gov.in.attendance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.audit.dto.AuditRecordRequest;
import com.maharecruitment.gov.in.audit.service.AuditTrailService;
import com.maharecruitment.gov.in.attendance.entity.HolidayMasterEntity;
import com.maharecruitment.gov.in.attendance.repository.HolidayRepository;
import com.maharecruitment.gov.in.attendance.repository.WeekOffWorkingDayRepository;
import com.maharecruitment.gov.in.common.service.CurrentActorProvider;

@ExtendWith(MockitoExtension.class)
class HolidayServiceImplTest {

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private AuditTrailService auditTrailService;

    @Mock
    private CurrentActorProvider currentActorProvider;

    @Mock
    private WeekOffWorkingDayRepository weekOffWorkingDayRepository;

    @Captor
    private ArgumentCaptor<AuditRecordRequest> auditRequestCaptor;

    private HolidayServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HolidayServiceImpl(
                holidayRepository,
                weekOffWorkingDayRepository,
                auditTrailService,
                currentActorProvider);
    }

    @Test
    void saveHolidayCreatesAuditEntryForWorkingDayHoliday() {
        HolidayMasterEntity holiday = new HolidayMasterEntity();
        holiday.setHolidayDate(LocalDate.of(2026, 5, 6));
        holiday.setHolidayName("Foundation Day");

        when(holidayRepository.findByHolidayDate(holiday.getHolidayDate())).thenReturn(Optional.empty());
        when(weekOffWorkingDayRepository.findByWorkingDate(holiday.getHolidayDate())).thenReturn(Optional.empty());
        when(currentActorProvider.getCurrentUserId()).thenReturn(99L);
        when(currentActorProvider.getCurrentActorEmail()).thenReturn("admin@mahait.org");
        when(holidayRepository.save(any(HolidayMasterEntity.class))).thenAnswer(invocation -> {
            HolidayMasterEntity saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        HolidayMasterEntity saved = service.saveHoliday(holiday);

        assertEquals(10L, saved.getId());
        verify(auditTrailService).record(auditRequestCaptor.capture());
        AuditRecordRequest request = auditRequestCaptor.getValue();
        assertEquals("CREATE", request.getActionType());
        assertEquals("10", request.getEntityId());
        assertEquals(true, request.getMetadata().get("workingDayHoliday"));
    }

    @Test
    void saveHolidayRejectsDuplicateHolidayDate() {
        HolidayMasterEntity existing = new HolidayMasterEntity();
        existing.setId(10L);
        existing.setHolidayDate(LocalDate.of(2026, 5, 6));
        existing.setHolidayName("Existing Holiday");

        HolidayMasterEntity duplicate = new HolidayMasterEntity();
        duplicate.setHolidayDate(existing.getHolidayDate());
        duplicate.setHolidayName("Another Holiday");

        when(holidayRepository.findByHolidayDate(existing.getHolidayDate())).thenReturn(Optional.of(existing));
        when(weekOffWorkingDayRepository.findByWorkingDate(existing.getHolidayDate())).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.saveHoliday(duplicate));

        assertEquals("A holiday is already configured for 2026-05-06.", exception.getMessage());
        verify(holidayRepository, never()).save(any(HolidayMasterEntity.class));
        verify(auditTrailService, never()).record(any(AuditRecordRequest.class));
    }

    @Test
    void saveHolidayRecordsUpdateAuditWhenValuesChange() {
        HolidayMasterEntity existing = new HolidayMasterEntity();
        existing.setId(10L);
        existing.setHolidayDate(LocalDate.of(2026, 5, 6));
        existing.setHolidayName("Foundation Day");

        HolidayMasterEntity update = new HolidayMasterEntity();
        update.setId(10L);
        update.setHolidayDate(LocalDate.of(2026, 5, 7));
        update.setHolidayName("State Foundation Day");

        when(holidayRepository.findByHolidayDate(update.getHolidayDate())).thenReturn(Optional.empty());
        when(weekOffWorkingDayRepository.findByWorkingDate(update.getHolidayDate())).thenReturn(Optional.empty());
        when(holidayRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(existing));
        when(currentActorProvider.getCurrentUserId()).thenReturn(99L);
        when(currentActorProvider.getCurrentActorEmail()).thenReturn("admin@mahait.org");
        when(holidayRepository.save(any(HolidayMasterEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HolidayMasterEntity saved = service.saveHoliday(update);

        assertEquals(LocalDate.of(2026, 5, 7), saved.getHolidayDate());
        verify(auditTrailService).record(auditRequestCaptor.capture());
        AuditRecordRequest request = auditRequestCaptor.getValue();
        assertEquals("UPDATE", request.getActionType());
        assertEquals(LocalDate.of(2026, 5, 6), request.getMetadata().get("previousHolidayDate"));
        assertEquals("Foundation Day", request.getMetadata().get("previousHolidayName"));
    }

    @Test
    void archiveHolidayMarksRecordInactiveAndRecordsAuditEntry() {
        HolidayMasterEntity holiday = new HolidayMasterEntity();
        holiday.setId(10L);
        holiday.setHolidayDate(LocalDate.of(2026, 5, 10));
        holiday.setHolidayName("Special Holiday");
        holiday.setActive(Boolean.TRUE);

        when(holidayRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(holiday));
        when(currentActorProvider.getCurrentUserId()).thenReturn(99L);
        when(currentActorProvider.getCurrentActorEmail()).thenReturn("admin@mahait.org");
        when(holidayRepository.save(any(HolidayMasterEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.archiveHoliday(10L);

        verify(holidayRepository).save(holiday);
        assertEquals(Boolean.FALSE, holiday.getActive());
        verify(auditTrailService).record(auditRequestCaptor.capture());
        AuditRecordRequest request = auditRequestCaptor.getValue();
        assertEquals("DEACTIVATE", request.getActionType());
        assertEquals("Special Holiday", request.getMetadata().get("holidayName"));
        assertEquals(Boolean.FALSE, request.getMetadata().get("active"));
    }

    @Test
    void getHolidayAuditTrailReturnsEmptyListForNullId() {
        assertEquals(List.of(), service.getHolidayAuditTrail(null));
    }
}
