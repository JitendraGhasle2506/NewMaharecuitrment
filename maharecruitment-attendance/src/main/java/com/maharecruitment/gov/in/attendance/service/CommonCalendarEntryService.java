package com.maharecruitment.gov.in.attendance.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import com.maharecruitment.gov.in.attendance.dto.CommonCalendarPhotoUpload;
import com.maharecruitment.gov.in.attendance.entity.CommonCalendarEntryEntity;
import com.maharecruitment.gov.in.attendance.entity.CommonCalendarEntryPhotoEntity;
import com.maharecruitment.gov.in.audit.dto.AuditEventView;

public interface CommonCalendarEntryService {
    List<CommonCalendarEntryEntity> getEntriesBetween(LocalDate startDate, LocalDate endDate);
    CommonCalendarEntryEntity getEntryById(Long id);
    CommonCalendarEntryEntity saveEntry(CommonCalendarEntryEntity entry);
    CommonCalendarEntryEntity saveEntryWithPhotos(
            CommonCalendarEntryEntity entry,
            List<CommonCalendarPhotoUpload> photos);
    List<CommonCalendarEntryPhotoEntity> getPhotosForEntries(Collection<Long> entryIds);
    long countActivePhotos(Long entryId);
    CommonCalendarEntryPhotoEntity archivePhoto(Long entryId, Long photoId);
    void archiveEntry(Long id);
    List<AuditEventView> getEntryAuditTrail(Long id);
}
