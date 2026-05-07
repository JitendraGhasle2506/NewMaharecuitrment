package com.maharecruitment.gov.in.attendance.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.maharecruitment.gov.in.attendance.entity.WeekOffWorkingDayEntity;
import com.maharecruitment.gov.in.audit.dto.AuditEventView;

public interface WeekOffWorkingDayService {

    List<WeekOffWorkingDayEntity> getWorkingDaysBetween(LocalDate startDate, LocalDate endDate);

    Set<LocalDate> getWorkingDayDatesBetween(LocalDate startDate, LocalDate endDate);

    WeekOffWorkingDayEntity getWorkingDayById(Long id);

    WeekOffWorkingDayEntity saveWorkingDay(WeekOffWorkingDayEntity workingDay);

    void archiveWorkingDay(Long id);

    List<AuditEventView> getWorkingDayAuditTrail(Long id);
}
