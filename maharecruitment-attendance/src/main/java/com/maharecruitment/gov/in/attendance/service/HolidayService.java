package com.maharecruitment.gov.in.attendance.service;

import java.util.List;
import com.maharecruitment.gov.in.attendance.entity.HolidayMasterEntity;

public interface HolidayService {
    List<HolidayMasterEntity> getAllHolidays();
    HolidayMasterEntity getHolidayById(Long id);
    HolidayMasterEntity saveHoliday(HolidayMasterEntity holiday);
    void deleteHoliday(Long id);
}
