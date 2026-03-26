package com.maharecruitment.gov.in.attendance.service;

import java.time.LocalDate;
import java.util.List;

import com.maharecruitment.gov.in.attendance.dto.AttendanceRegisterDTO;
import com.maharecruitment.gov.in.attendance.dto.AttendanceReportDTO;
import com.maharecruitment.gov.in.attendance.dto.ManualAttendanceRequestDTO;

public interface AttendanceRegisterService {
	public AttendanceRegisterDTO getEmployeeDetails(Long id, LocalDate startDate, LocalDate endDate);

	public List<AttendanceRegisterDTO> getInternalAttendance(Long departmentId, int month, int year);
	public AttendanceRegisterDTO getInternalAttendanceForEmployee(Long employeeId, int month, int year);

	public List<AttendanceRegisterDTO> getExternalAttendance(Long departmentId, int month, int year);

	public void saveExternalAttendance(List<AttendanceRegisterDTO> dtos);

	List<AttendanceReportDTO> getAttendanceReportData(Long regId, Long subDeptId, Integer month, Integer year,
			Long projectId);

	List<AttendanceReportDTO> getExternalAttendanceReportData(Long regId, Long agencyId, Integer month, Integer year,
			Long projectId);

	boolean isAttendanceLocked(Long departmentId, int month, int year);

	public void lockAttendance(Long departmentId, int month, int year);

	// Manual Attendance Flow
	public boolean submitManualAttendance(Long userId, LocalDate date, String inTime, String outTime, String reason);
	public List<ManualAttendanceRequestDTO> getPendingRequests(Long approverId, String roleType);
    
    public List<ManualAttendanceRequestDTO> getMyManualRequests(Long employeeId);
	public void approveRejectManualAttendance(Long requestId, Long approverId, String status, String comments, String roleType);

    public List<java.util.Map<String, Object>> getTeamMembers(Long approverId, String roleType);

    public List<com.maharecruitment.gov.in.attendance.dto.ManualAttendanceSummaryDTO> getPendingSummaries(Long approverId, String roleType);
    public List<ManualAttendanceRequestDTO> getPendingRequestsForEmployee(Long approverId, Long targetEmployeeId, String roleType);
}
