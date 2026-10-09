package com.maharecruitment.gov.in.attendance.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maharecruitment.gov.in.attendance.entity.CommonCalendarEntryPhotoEntity;

public interface CommonCalendarEntryPhotoRepository extends JpaRepository<CommonCalendarEntryPhotoEntity, Long> {

    List<CommonCalendarEntryPhotoEntity> findByCalendarEntryIdInAndActiveTrueOrderByDisplayOrderAscIdAsc(
            Collection<Long> calendarEntryIds);

    List<CommonCalendarEntryPhotoEntity> findByCalendarEntryIdAndActiveTrueOrderByDisplayOrderAscIdAsc(
            Long calendarEntryId);

    long countByCalendarEntryIdAndActiveTrue(Long calendarEntryId);

    Optional<CommonCalendarEntryPhotoEntity> findByIdAndCalendarEntryIdAndActiveTrue(Long id, Long calendarEntryId);
}
