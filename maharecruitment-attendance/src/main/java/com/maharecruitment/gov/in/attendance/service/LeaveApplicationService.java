package com.maharecruitment.gov.in.attendance.service;

import java.util.List;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.attendance.dto.LeaveApplicationHODDTO;
import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;

public interface LeaveApplicationService {

    void saveLeaveApplication(LeaveApplicationEntity leaveApplication);

    List<LeaveApplicationEntity> getLeaveApplicationsByEmployee(Long employeeId);

    List<LeaveApplicationHODDTO> getPendingLeavesForHOD(Long hodUserId, String search);
    List<LeaveApplicationHODDTO> getPendingLeavesForHOD(Long hodUserId, String search, LocalDate searchDate);
    List<LeaveApplicationHODDTO> getProcessedLeavesForHOD(Long hodUserId, String search);
    Page<LeaveApplicationHODDTO> getProcessedLeavesForHOD(
            Long hodUserId, String search, LocalDate searchDate, Pageable pageable);

    void updateLeaveStatus(Long leaveId, String status, String remarks, Long actorUserId);

    boolean isValidCompOffWorkedDate(Long employeeId, LocalDate workedDate);

    void cancelLeaveApplication(Long leaveId, Long employeeId);
}
