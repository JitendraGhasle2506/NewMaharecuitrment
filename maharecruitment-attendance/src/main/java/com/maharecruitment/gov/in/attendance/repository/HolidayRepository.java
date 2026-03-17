package com.maharecruitment.gov.in.attendance.repository;


import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.attendance.entity.HolidayMasterEntity;


@Repository
public interface HolidayRepository extends JpaRepository<HolidayMasterEntity, Long> {

	List<HolidayMasterEntity> findByHolidayDateBetween(LocalDate startDate, LocalDate endDate);
}
