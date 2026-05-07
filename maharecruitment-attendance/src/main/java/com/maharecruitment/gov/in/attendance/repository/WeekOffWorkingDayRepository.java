package com.maharecruitment.gov.in.attendance.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.attendance.entity.WeekOffWorkingDayEntity;

@Repository
public interface WeekOffWorkingDayRepository extends JpaRepository<WeekOffWorkingDayEntity, Long> {

    @Query("""
            select workingDay
            from WeekOffWorkingDayEntity workingDay
            where workingDay.active = true
              and workingDay.workingDate between :startDate and :endDate
            order by workingDay.workingDate asc
            """)
    List<WeekOffWorkingDayEntity> findByWorkingDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            select workingDay
            from WeekOffWorkingDayEntity workingDay
            where workingDay.active = true
              and workingDay.workingDate = :workingDate
            """)
    Optional<WeekOffWorkingDayEntity> findByWorkingDate(@Param("workingDate") LocalDate workingDate);

    Optional<WeekOffWorkingDayEntity> findByIdAndActiveTrue(Long id);
}
