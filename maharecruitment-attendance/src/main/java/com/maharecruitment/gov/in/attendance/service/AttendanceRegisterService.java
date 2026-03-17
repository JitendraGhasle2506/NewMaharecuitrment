package com.maharecruitment.gov.in.attendance.service;

import java.time.LocalDate;
import java.util.List;

import com.maharecruitment.gov.in.attendance.dto.AttendanceRegisterDTO;
import com.maharecruitment.gov.in.attendance.dto.AttendanceReportDTO;

public interface AttendanceRegisterService {
	public AttendanceRegisterDTO getEmployeeDetails(Long id, LocalDate startDate, LocalDate endDate);

	public List<AttendanceRegisterDTO> getExternalAttendance(Long departmentId, int month, int year);

	public void saveExternalAttendance(List<AttendanceRegisterDTO> dtos);

	List<AttendanceReportDTO> getAttendanceReportData(Long regId, Long subDeptId, Integer month, Integer year,
			Long projectId);

	List<AttendanceReportDTO> getExternalAttendanceReportData(Long regId, Long agencyId, Integer month, Integer year,
			Long projectId);

	boolean isAttendanceLocked(Long departmentId, int month, int year);

	public void lockAttendance(Long departmentId, int month, int year);
}
