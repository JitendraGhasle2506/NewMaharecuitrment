package com.maharecruitment.gov.in.attendance.repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.attendance.entity.HolidayMasterEntity;


@Repository
public interface HolidayRepository extends JpaRepository<HolidayMasterEntity, Long> {

	@Query("""
			select holiday
			from HolidayMasterEntity holiday
			where holiday.active = true
			  and holiday.holidayDate between :startDate and :endDate
			order by holiday.holidayDate asc
			""")
	List<HolidayMasterEntity> findByHolidayDateBetween(
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	@Query("""
			select holiday
			from HolidayMasterEntity holiday
			where holiday.active = true
			order by holiday.holidayDate asc
			""")
	List<HolidayMasterEntity> findAllByOrderByHolidayDateAsc();

	@Query("""
			select holiday
			from HolidayMasterEntity holiday
			where holiday.active = true
			  and holiday.holidayDate = :holidayDate
			""")
	Optional<HolidayMasterEntity> findByHolidayDate(@Param("holidayDate") LocalDate holidayDate);

	Optional<HolidayMasterEntity> findByIdAndActiveTrue(Long id);
}
