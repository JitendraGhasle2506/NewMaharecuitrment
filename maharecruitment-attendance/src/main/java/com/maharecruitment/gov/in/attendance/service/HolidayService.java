package com.maharecruitment.gov.in.attendance.service;

import java.time.LocalDate;
import java.util.List;

import com.maharecruitment.gov.in.audit.dto.AuditEventView;
import com.maharecruitment.gov.in.attendance.entity.HolidayMasterEntity;

public interface HolidayService {
    List<HolidayMasterEntity> getAllHolidays();
    List<HolidayMasterEntity> getHolidaysBetween(LocalDate startDate, LocalDate endDate);
    HolidayMasterEntity getHolidayById(Long id);
    HolidayMasterEntity saveHoliday(HolidayMasterEntity holiday);
    void archiveHoliday(Long id);
    List<AuditEventView> getHolidayAuditTrail(Long id);
}
