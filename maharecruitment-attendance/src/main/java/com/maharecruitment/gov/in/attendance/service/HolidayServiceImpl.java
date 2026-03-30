package com.maharecruitment.gov.in.attendance.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.maharecruitment.gov.in.attendance.entity.HolidayMasterEntity;
import com.maharecruitment.gov.in.attendance.repository.HolidayRepository;

@Service
@Transactional
public class HolidayServiceImpl implements HolidayService {

    @Autowired
    private HolidayRepository holidayRepository;

    @Override
    public List<HolidayMasterEntity> getAllHolidays() {
        return holidayRepository.findAll();
    }

    @Override
    public HolidayMasterEntity getHolidayById(Long id) {
        return holidayRepository.findById(id).orElse(null);
    }

    @Override
    public HolidayMasterEntity saveHoliday(HolidayMasterEntity holiday) {
        return holidayRepository.save(holiday);
    }

    @Override
    public void deleteHoliday(Long id) {
        holidayRepository.deleteById(id);
    }
}
