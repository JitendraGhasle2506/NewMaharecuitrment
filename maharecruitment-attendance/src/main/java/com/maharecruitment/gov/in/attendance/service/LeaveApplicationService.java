package com.maharecruitment.gov.in.attendance.service;

import java.util.List;

import java.util.List;
import com.maharecruitment.gov.in.attendance.dto.LeaveApplicationHODDTO;
import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;

public interface LeaveApplicationService {

    void saveLeaveApplication(LeaveApplicationEntity leaveApplication);

    List<LeaveApplicationEntity> getLeaveApplicationsByEmployee(Long employeeId);

    List<LeaveApplicationHODDTO> getPendingLeavesForHOD(Long hodUserId, String search);
    List<LeaveApplicationHODDTO> getProcessedLeavesForHOD(Long hodUserId, String search);

    void updateLeaveStatus(Long leaveId, String status, String remarks);
}
