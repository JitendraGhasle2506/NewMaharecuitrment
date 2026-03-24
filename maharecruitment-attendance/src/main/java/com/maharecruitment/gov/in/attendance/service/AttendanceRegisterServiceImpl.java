package com.maharecruitment.gov.in.attendance.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.attendance.dto.AttendanceDayDTO;
import com.maharecruitment.gov.in.attendance.dto.AttendanceRegisterDTO;
import com.maharecruitment.gov.in.attendance.dto.AttendanceReportDTO;
import com.maharecruitment.gov.in.attendance.dto.AttendanceReportProjection;
import com.maharecruitment.gov.in.attendance.entity.AttendanceRegisterEntity;
import com.maharecruitment.gov.in.attendance.entity.DailyAttendanceInternalEntity;
import com.maharecruitment.gov.in.attendance.entity.HolidayMasterEntity;
import com.maharecruitment.gov.in.attendance.repository.AttendanceLockRepository;
import com.maharecruitment.gov.in.attendance.repository.AttendanceRegisterRepo;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.attendance.repository.HolidayRepository;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationEntity;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class AttendanceRegisterServiceImpl implements AttendanceRegisterService {

	@Autowired
	private EmployeeRepository employeeRepository;
	@Autowired
	private AttendanceRegisterRepo attendanceRegisterRepo;
	@Autowired
	private HolidayRepository holidayRepository;
	@Autowired

	private DailyAttendanceInternalRepository dailyAttendanceInternalRepository;

	@Autowired
	private DepartmentProjectApplicationRepository departmentProjectApplicationRepository;

	@Autowired
	private AttendanceLockRepository attendanceLockRepository;

	@Override
	public AttendanceRegisterDTO getEmployeeDetails(Long id,
			LocalDate startDate,
			LocalDate endDate) {

		EmployeeEntity emp = employeeRepository.findById(id)
				.orElseThrow(() -> new RuntimeException(
						"Employee not found with user id : " + id));

		int month = startDate.getMonthValue();
		int year = startDate.getYear();

		Optional<AttendanceRegisterEntity> attendanceOpt = attendanceRegisterRepo.findByUserIdAndMonthAndYear(id, month,
				year);

		List<HolidayMasterEntity> holidays = holidayRepository.findByHolidayDateBetween(startDate, endDate);

		Set<LocalDate> holidayDates = holidays.stream()
				.map(HolidayMasterEntity::getHolidayDate)
				.collect(Collectors.toSet());

		List<AttendanceDayDTO> finalList = new ArrayList<>();
		LocalDate today = LocalDate.now();
		double maxOutHour = 0;

		for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {

			AttendanceDayDTO dayDTO = new AttendanceDayDTO();
			dayDTO.setDate(date);

			String status = null;
			if (attendanceOpt.isPresent()) {
				status = getDayStatus(attendanceOpt.get(), date.getDayOfMonth());
			}

			if (status != null && !status.isEmpty()) {
				dayDTO.setStatus(status);
			} else if (date.isAfter(today)) {
				dayDTO.setStatus("FUTURE");
			} else if (holidayDates.contains(date)) {
				dayDTO.setStatus("HOLIDAY");
			} else if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
				dayDTO.setStatus("WEEK_OFF");
			} else {
				dayDTO.setStatus("PRESENT");
			}

			finalList.add(dayDTO);
		}

		maxOutHour = 0.5;

		List<AttendanceDayDTO> presentDays = finalList.stream()
				.filter(d -> "PRESENT".equals(d.getStatus()))
				.collect(Collectors.toList());

		AttendanceRegisterDTO dto = new AttendanceRegisterDTO();
		dto.setName(emp.getFullName());
		dto.setDesignation(emp.getDesignation() != null ? emp.getDesignation().getDesignationName() : null);
		dto.setEmail(emp.getEmail());
		dto.setMobile(emp.getMobile());
		dto.setOrganization(emp.getAgency() != null ? emp.getAgency().getAgencyName() : null);
		dto.setUserId(emp.getEmployeeId());
		dto.setAttendanceDays(finalList);
		dto.setMaxOutHour(maxOutHour);
		dto.setPresentDays(presentDays);
		dto.setTodayInTime("-");
		dto.setTodayOutTime("-");
		dto.setRequestId(emp.getRequestId());
		dto.setLevel(emp.getLevelCode());

		return dto;
	}

	private String getDayStatus(AttendanceRegisterEntity entity, int day) {
		switch (day) {
			case 1:
				return entity.getD1();
			case 2:
				return entity.getD2();
			case 3:
				return entity.getD3();
			case 4:
				return entity.getD4();
			case 5:
				return entity.getD5();
			case 6:
				return entity.getD6();
			case 7:
				return entity.getD7();
			case 8:
				return entity.getD8();
			case 9:
				return entity.getD9();
			case 10:
				return entity.getD10();
			case 11:
				return entity.getD11();
			case 12:
				return entity.getD12();
			case 13:
				return entity.getD13();
			case 14:
				return entity.getD14();
			case 15:
				return entity.getD15();
			case 16:
				return entity.getD16();
			case 17:
				return entity.getD17();
			case 18:
				return entity.getD18();
			case 19:
				return entity.getD19();
			case 20:
				return entity.getD20();
			case 21:
				return entity.getD21();
			case 22:
				return entity.getD22();
			case 23:
				return entity.getD23();
			case 24:
				return entity.getD24();
			case 25:
				return entity.getD25();
			case 26:
				return entity.getD26();
			case 27:
				return entity.getD27();
			case 28:
				return entity.getD28();
			case 29:
				return entity.getD29();
			case 30:
				return entity.getD30();
			case 31:
				return entity.getD31();
			default:
				return null;
		}
	}

	@Override
	public List<AttendanceRegisterDTO> getInternalAttendance(Long departmentId, int month, int year) {
		List<EmployeeEntity> employees = employeeRepository
				.findByDepartmentRegistration_DepartmentRegistrationIdAndRecruitmentType(departmentId, "INTERNAL");

		YearMonth yearMonth = YearMonth.of(year, month);
		LocalDate startDate = yearMonth.atDay(1);
		LocalDate endDate = yearMonth.atEndOfMonth();

		List<DailyAttendanceInternalEntity> dailyAttendance = dailyAttendanceInternalRepository
				.findByAttendanceDateBetween(startDate, endDate);

		List<DepartmentProjectApplicationEntity> projects = departmentProjectApplicationRepository
				.findByDepartmentRegistrationIdOrderByDepartmentProjectApplicationIdDesc(departmentId);

		Map<String, String> projectMap = projects.stream()
				.collect(Collectors.toMap(DepartmentProjectApplicationEntity::getRequestId,
						DepartmentProjectApplicationEntity::getProjectName, (existing, replacement) -> existing));

		Map<Long, Map<LocalDate, DailyAttendanceInternalEntity>> attendanceMap = dailyAttendance.stream()
				.collect(Collectors.groupingBy(DailyAttendanceInternalEntity::getEmployeeId,
						Collectors.toMap(DailyAttendanceInternalEntity::getAttendanceDate, a -> a)));

		List<AttendanceRegisterDTO> resultList = new ArrayList<>();
		for (EmployeeEntity employee : employees) {
			AttendanceRegisterDTO dto = new AttendanceRegisterDTO();
			dto.setEmpId(employee.getEmployeeId());
			dto.setUserId(employee.getEmployeeId());
			dto.setName(employee.getFullName());
			dto.setDesignation(employee.getDesignation() != null ? employee.getDesignation().getDesignationName() : null);
			dto.setRequestId(employee.getRequestId());
			dto.setLevel(employee.getLevelCode());
			dto.setOrganization(employee.getAgency() != null ? employee.getAgency().getAgencyName() : null);
			dto.setProjectName(employee.getRequestId() != null ? projectMap.get(employee.getRequestId()) : null);

			Map<LocalDate, DailyAttendanceInternalEntity> empAttendance = attendanceMap
					.getOrDefault(employee.getEmployeeId(), new HashMap<>());

			List<AttendanceDayDTO> days = new ArrayList<>();
			for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
				AttendanceDayDTO dayDTO = new AttendanceDayDTO();
				dayDTO.setDate(date);
				DailyAttendanceInternalEntity daily = empAttendance.get(date);
				if (daily != null) {
					dayDTO.setInTime(daily.getInTime());
					dayDTO.setOutTime(daily.getOutTime());
					dayDTO.setStayHours(daily.getTotalHours());
					dayDTO.setStatus(daily.getStatus());
				} else {
					dayDTO.setStatus("");
				}
				days.add(dayDTO);
			}
			dto.setAttendanceDays(days);
			resultList.add(dto);
		}
		return resultList;
	}

	@Override
	public AttendanceRegisterDTO getInternalAttendanceForEmployee(Long employeeId, int month, int year) {
		EmployeeEntity employee = employeeRepository.findById(employeeId)
				.orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

		YearMonth yearMonth = YearMonth.of(year, month);
		LocalDate startDate = yearMonth.atDay(1);
		LocalDate endDate = yearMonth.atEndOfMonth();

		List<DailyAttendanceInternalEntity> dailyAttendance = dailyAttendanceInternalRepository
				.findByEmployeeIdAndMonthAndYear(employeeId, month, year);

		Map<LocalDate, DailyAttendanceInternalEntity> empAttendance = dailyAttendance.stream()
				.collect(Collectors.toMap(DailyAttendanceInternalEntity::getAttendanceDate, a -> a));

		List<HolidayMasterEntity> holidays = holidayRepository.findByHolidayDateBetween(startDate, endDate);
		Set<LocalDate> holidayDates = holidays.stream()
				.map(HolidayMasterEntity::getHolidayDate)
				.collect(Collectors.toSet());

		Long departmentId = employee.getDepartmentRegistration() != null ? employee.getDepartmentRegistration().getDepartmentRegistrationId() : null;
		
		Map<String, String> projectMap = new HashMap<>();
		if (departmentId != null) {
			List<DepartmentProjectApplicationEntity> projects = departmentProjectApplicationRepository
					.findByDepartmentRegistrationIdOrderByDepartmentProjectApplicationIdDesc(departmentId);

			projectMap = projects.stream()
					.collect(Collectors.toMap(DepartmentProjectApplicationEntity::getRequestId,
							DepartmentProjectApplicationEntity::getProjectName, (existing, replacement) -> existing));
		}

		AttendanceRegisterDTO dto = new AttendanceRegisterDTO();
		dto.setEmpId(employee.getEmployeeId());
		dto.setUserId(employee.getEmployeeId());
		dto.setName(employee.getFullName());
		dto.setDesignation(employee.getDesignation() != null ? employee.getDesignation().getDesignationName() : null);
		dto.setRequestId(employee.getRequestId());
		dto.setLevel(employee.getLevelCode());
		dto.setOrganization(employee.getAgency() != null ? employee.getAgency().getAgencyName() : null);
		dto.setProjectName(employee.getRequestId() != null ? projectMap.get(employee.getRequestId()) : null);
		
		// New UI Fields
		dto.setDivision(employee.getSubDepartment() != null ? employee.getSubDepartment().getSubDeptName() : "-");
		dto.setOfficeLocation(employee.getDepartmentRegistration() != null ? employee.getDepartmentRegistration().getAddress() : "-");
		dto.setEmail(employee.getEmail());
		dto.setMobile(employee.getMobile());

		// Today's Activity
		LocalDate today = LocalDate.now();
		DailyAttendanceInternalEntity todayAttendance = dailyAttendanceInternalRepository
				.findByEmployeeIdAndAttendanceDate(employeeId, today).orElse(null);
		if (todayAttendance != null) {
			dto.setTodayInTime(todayAttendance.getInTime());
			dto.setTodayOutTime(todayAttendance.getOutTime());
			dto.setTodayStatus(todayAttendance.getStatus());
		} else {
			dto.setTodayInTime("-");
			dto.setTodayOutTime("-");
		}
		dto.setAvgResponseTime("-"); // Placeholder

		List<AttendanceDayDTO> days = new ArrayList<>();
		for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
			AttendanceDayDTO dayDTO = new AttendanceDayDTO();
			dayDTO.setDate(date);
			DailyAttendanceInternalEntity daily = empAttendance.get(date);
			if (daily != null) {
				dayDTO.setInTime(daily.getInTime());
				dayDTO.setOutTime(daily.getOutTime());
				dayDTO.setStayHours(daily.getTotalHours());
				dayDTO.setStatus(daily.getStatus());
			} else if (holidayDates.contains(date)) {
				dayDTO.setStatus("HOLIDAY");
			} else if (!date.isAfter(LocalDate.now())) {
				if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
					dayDTO.setStatus("WEEK_OFF");
				} else {
					dayDTO.setStatus("ABSENT");
				}
			} else {
				dayDTO.setStatus("");
			}
			days.add(dayDTO);
		}
		dto.setAttendanceDays(days);
		return dto;
	}

	@Override
	public List<AttendanceRegisterDTO> getExternalAttendance(Long departmentId, int month, int year) {
		List<EmployeeEntity> employees = employeeRepository
				.findByDepartmentRegistration_DepartmentRegistrationIdAndRecruitmentType(departmentId, "EXTERNAL");
		YearMonth yearMonth = YearMonth.of(year, month);
		LocalDate startDate = yearMonth.atDay(1);
		LocalDate endDate = yearMonth.atEndOfMonth();

		LocalDate today = LocalDate.now();
		if (yearMonth.equals(YearMonth.now())) {
			endDate = today;
		} else if (yearMonth.isAfter(YearMonth.now())) {
			return new ArrayList<>();
		}

		List<AttendanceRegisterDTO> resultList = new ArrayList<>();
		for (EmployeeEntity employee : employees) {
			AttendanceRegisterDTO dto = getEmployeeDetails(employee.getEmployeeId(), startDate, endDate);
			dto.setEmpId(employee.getEmployeeId());
			resultList.add(dto);
		}
		return resultList;
	}

	@Override
	public void saveExternalAttendance(List<AttendanceRegisterDTO> dtos) {
		for (AttendanceRegisterDTO dto : dtos) {
			if (dto.getAttendanceDays() != null && !dto.getAttendanceDays().isEmpty()) {
				LocalDate firstDate = dto.getAttendanceDays().get(0).getDate();
				int month = firstDate.getMonthValue();
				int year = firstDate.getYear();

				AttendanceRegisterEntity entity = attendanceRegisterRepo
						.findByUserIdAndMonthAndYear(dto.getUserId(), month, year)
						.orElse(new AttendanceRegisterEntity());

				entity.setUserId(dto.getUserId());
				entity.setMonth(month);
				entity.setYear(year);
				entity.setRequestId(dto.getRequestId());
				entity.setDesignation(dto.getDesignation());
				entity.setName(dto.getName());

				for (AttendanceDayDTO dayDTO : dto.getAttendanceDays()) {
					if (dayDTO.getStatus() != null) {
						setDayStatus(entity, dayDTO.getDate().getDayOfMonth(), dayDTO.getStatus());
					}
				}
				attendanceRegisterRepo.save(entity);
			}
		}
	}

	private void setDayStatus(AttendanceRegisterEntity entity, int day, String status) {
		switch (day) {
			case 1:
				entity.setD1(status);
				break;
			case 2:
				entity.setD2(status);
				break;
			case 3:
				entity.setD3(status);
				break;
			case 4:
				entity.setD4(status);
				break;
			case 5:
				entity.setD5(status);
				break;
			case 6:
				entity.setD6(status);
				break;
			case 7:
				entity.setD7(status);
				break;
			case 8:
				entity.setD8(status);
				break;
			case 9:
				entity.setD9(status);
				break;
			case 10:
				entity.setD10(status);
				break;
			case 11:
				entity.setD11(status);
				break;
			case 12:
				entity.setD12(status);
				break;
			case 13:
				entity.setD13(status);
				break;
			case 14:
				entity.setD14(status);
				break;
			case 15:
				entity.setD15(status);
				break;
			case 16:
				entity.setD16(status);
				break;
			case 17:
				entity.setD17(status);
				break;
			case 18:
				entity.setD18(status);
				break;
			case 19:
				entity.setD19(status);
				break;
			case 20:
				entity.setD20(status);
				break;
			case 21:
				entity.setD21(status);
				break;
			case 22:
				entity.setD22(status);
				break;
			case 23:
				entity.setD23(status);
				break;
			case 24:
				entity.setD24(status);
				break;
			case 25:
				entity.setD25(status);
				break;
			case 26:
				entity.setD26(status);
				break;
			case 27:
				entity.setD27(status);
				break;
			case 28:
				entity.setD28(status);
				break;
			case 29:
				entity.setD29(status);
				break;
			case 30:
				entity.setD30(status);
				break;
			case 31:
				entity.setD31(status);
				break;
		}
	}

	@Override
	public List<AttendanceReportDTO> getAttendanceReportData(Long regId, Long subDeptId, Integer month, Integer year,
			Long projectId) {

		LocalDate startDate = LocalDate.of(year, month, 1);
		LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
		List<HolidayMasterEntity> holidays = holidayRepository.findByHolidayDateBetween(startDate, endDate);
		Set<Integer> holidayDays = holidays.stream()
				.map(h -> h.getHolidayDate().getDayOfMonth())
				.collect(Collectors.toSet());

		List<AttendanceReportProjection> projections = attendanceRegisterRepo.getAttendanceReportNative(regId,
				subDeptId, projectId, month, year);

		return projections.stream().map(p -> {
			AttendanceReportDTO dto = new AttendanceReportDTO();
			dto.setUserId(p.getUserId());
			dto.setRequestId(p.getRequestId());
			dto.setEmployeeName(p.getFullName());
			dto.setDepartment(p.getDepartmentName());
			dto.setSubDepartment(p.getSubDepartmentName());
			dto.setProjectName(p.getApplicationName());

			Map<Integer, String> dailyStatus = new HashMap<>();
			for (int day = 1; day <= 31; day++) {
				String status = getProjectionDayStatus(p, day);
				if (status != null && !status.isEmpty()) {
					dailyStatus.put(day, mapStatusToCode(status));
				} else if (holidayDays.contains(day)) {
					dailyStatus.put(day, "H");
				} else {
					dailyStatus.put(day, "");
				}
			}
			dto.setDailyStatus(dailyStatus);
			return dto;
		}).collect(Collectors.toList());
	}

	private String getProjectionDayStatus(AttendanceReportProjection p, int day) {
		switch (day) {
			case 1:
				return p.getD1();
			case 2:
				return p.getD2();
			case 3:
				return p.getD3();
			case 4:
				return p.getD4();
			case 5:
				return p.getD5();
			case 6:
				return p.getD6();
			case 7:
				return p.getD7();
			case 8:
				return p.getD8();
			case 9:
				return p.getD9();
			case 10:
				return p.getD10();
			case 11:
				return p.getD11();
			case 12:
				return p.getD12();
			case 13:
				return p.getD13();
			case 14:
				return p.getD14();
			case 15:
				return p.getD15();
			case 16:
				return p.getD16();
			case 17:
				return p.getD17();
			case 18:
				return p.getD18();
			case 19:
				return p.getD19();
			case 20:
				return p.getD20();
			case 21:
				return p.getD21();
			case 22:
				return p.getD22();
			case 23:
				return p.getD23();
			case 24:
				return p.getD24();
			case 25:
				return p.getD25();
			case 26:
				return p.getD26();
			case 27:
				return p.getD27();
			case 28:
				return p.getD28();
			case 29:
				return p.getD29();
			case 30:
				return p.getD30();
			case 31:
				return p.getD31();
			default:
				return null;
		}
	}

	private String mapStatusToCode(String status) {
		if (status == null)
			return "";
		switch (status.toUpperCase()) {
			case "PRESENT":
				return "P";
			case "ABSENT":
				return "A";
			case "WEEK_OFF":
				return "W";
			case "LEAVE":
				return "L";
			case "TOUR":
				return "T";
			case "HOLIDAY":
				return "H";
			default:
				return status;
		}
	}

	@Override
	public List<AttendanceReportDTO> getExternalAttendanceReportData(Long regId, Long agencyId, Integer month,
			Integer year,
			Long projectId) {

		LocalDate startDate = LocalDate.of(year, month, 1);
		LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
		List<HolidayMasterEntity> holidays = holidayRepository.findByHolidayDateBetween(startDate, endDate);
		Set<Integer> holidayDays = holidays.stream()
				.map(h -> h.getHolidayDate().getDayOfMonth())
				.collect(Collectors.toSet());

		List<com.maharecruitment.gov.in.attendance.dto.ExternalAttendanceReportProjection> projections = attendanceRegisterRepo
				.getExternalAttendanceReportNative(regId,
						agencyId, projectId, month, year);

		return projections.stream().map(p -> {
			AttendanceReportDTO dto = new AttendanceReportDTO();
			dto.setUserId(p.getUserId());
			dto.setRequestId(p.getRequestId());
			dto.setEmployeeName(p.getFullName());
			dto.setDepartment(p.getDepartmentName());
			dto.setAgencyName(p.getSubDepartmentName()); // Aliased as subDepartmentName in the query
			dto.setProjectName(p.getApplicationName());
			dto.setLevel(p.getLevel());
			dto.setDesignation(p.getDesignation());

			Map<Integer, String> dailyStatus = new HashMap<>();
			for (int day = 1; day <= 31; day++) {
				String status = getExternalProjectionDayStatus(p, day);
				if (status != null && !status.isEmpty()) {
					dailyStatus.put(day, mapStatusToCode(status));
				} else if (holidayDays.contains(day)) {
					dailyStatus.put(day, "H");
				} else {
					dailyStatus.put(day, "");
				}
			}
			dto.setDailyStatus(dailyStatus);
			return dto;
		}).collect(Collectors.toList());
	}

	private String getExternalProjectionDayStatus(
			com.maharecruitment.gov.in.attendance.dto.ExternalAttendanceReportProjection p, int day) {
		switch (day) {
			case 1:
				return p.getD1();
			case 2:
				return p.getD2();
			case 3:
				return p.getD3();
			case 4:
				return p.getD4();
			case 5:
				return p.getD5();
			case 6:
				return p.getD6();
			case 7:
				return p.getD7();
			case 8:
				return p.getD8();
			case 9:
				return p.getD9();
			case 10:
				return p.getD10();
			case 11:
				return p.getD11();
			case 12:
				return p.getD12();
			case 13:
				return p.getD13();
			case 14:
				return p.getD14();
			case 15:
				return p.getD15();
			case 16:
				return p.getD16();
			case 17:
				return p.getD17();
			case 18:
				return p.getD18();
			case 19:
				return p.getD19();
			case 20:
				return p.getD20();
			case 21:
				return p.getD21();
			case 22:
				return p.getD22();
			case 23:
				return p.getD23();
			case 24:
				return p.getD24();
			case 25:
				return p.getD25();
			case 26:
				return p.getD26();
			case 27:
				return p.getD27();
			case 28:
				return p.getD28();
			case 29:
				return p.getD29();
			case 30:
				return p.getD30();
			case 31:
				return p.getD31();
			default:
				return null;
		}
	}

	@Override
	public void lockAttendance(Long departmentId, int month, int year) {
		com.maharecruitment.gov.in.attendance.entity.AttendanceLockEntity lock = attendanceLockRepository
				.findByDeptRegIdAndMonthAndYear(departmentId, month, year)
				.orElse(new com.maharecruitment.gov.in.attendance.entity.AttendanceLockEntity());

		lock.setDeptRegId(departmentId);
		lock.setMonth(month);
		lock.setYear(year);
		lock.setLocked(true);
		lock.setLockedBy(1L);
		lock.setLockedAt(java.time.LocalDateTime.now());

		attendanceLockRepository.save(lock);
	}

	@Override
	public boolean isAttendanceLocked(Long departmentId, int month, int year) {
		Optional<com.maharecruitment.gov.in.attendance.entity.AttendanceLockEntity> lockOpt = attendanceLockRepository
				.findByDeptRegIdAndMonthAndYear(departmentId, month, year);

		if (lockOpt.isPresent() && lockOpt.get().isLocked()) {
			return true;
		}

		// 2. Check automatic deadline (cutoff: 15th of the following month)
		LocalDate today = LocalDate.now();
		LocalDate cutoffDate = LocalDate.of(year, month, 1).plusMonths(1).withDayOfMonth(15);

		if (today.isAfter(cutoffDate)) {
			// Only lock if data exists in database (as per user request: "data which is not
			// there it will not be locked")
			return attendanceRegisterRepo.existsByDeptRegIdAndMonthAndYear(departmentId, month, year);
		}

		return false;
	}
}
