package com.maharecruitment.gov.in.attendance.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.maharecruitment.gov.in.attendance.entity.CalendarEntryType;
import com.maharecruitment.gov.in.attendance.entity.CommonCalendarEntryEntity;

public interface CommonCalendarEntryRepository extends JpaRepository<CommonCalendarEntryEntity, Long> {

    @Query("""
            select entry
            from CommonCalendarEntryEntity entry
            where entry.active = true
              and entry.entryDate between :startDate and :endDate
            order by entry.entryDate asc, entry.entryType asc, entry.title asc
            """)
    List<CommonCalendarEntryEntity> findActiveBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    Optional<CommonCalendarEntryEntity> findByIdAndActiveTrue(Long id);

    @Query("""
            select count(entry) > 0
            from CommonCalendarEntryEntity entry
            where entry.active = true
              and entry.entryDate = :entryDate
              and entry.entryType = :entryType
              and lower(entry.title) = lower(:title)
              and (:excludedId is null or entry.id <> :excludedId)
            """)
    boolean existsDuplicate(
            @Param("entryDate") LocalDate entryDate,
            @Param("entryType") CalendarEntryType entryType,
            @Param("title") String title,
            @Param("excludedId") Long excludedId);
}
