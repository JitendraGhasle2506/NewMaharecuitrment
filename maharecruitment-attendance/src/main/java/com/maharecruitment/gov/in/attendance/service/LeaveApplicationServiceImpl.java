package com.maharecruitment.gov.in.attendance.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.attendance.dto.LeaveApplicationHODDTO;
import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;
import com.maharecruitment.gov.in.attendance.repository.LeaveApplicationRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingMappingRepository;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class LeaveApplicationServiceImpl implements LeaveApplicationService {

    @Autowired
    private LeaveApplicationRepository leaveApplicationRepository;

    @Autowired
    private EmployeeReportingMappingRepository employeeReportingMappingRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public void saveLeaveApplication(LeaveApplicationEntity leaveApplication) {
        if (leaveApplication.getApplicationDate() == null) {
            leaveApplication.setApplicationDate(LocalDateTime.now());
        }
        if (leaveApplication.getStatus() == null) {
            leaveApplication.setStatus("PENDING");
        }
        leaveApplicationRepository.save(leaveApplication);
    }

    @Override
    public List<LeaveApplicationEntity> getLeaveApplicationsByEmployee(Long employeeId) {
        return leaveApplicationRepository.findByEmployeeIdOrderByApplicationDateDesc(employeeId);
    }

    @Override
    public List<LeaveApplicationHODDTO> getPendingLeavesForHOD(Long hodUserId) {
        List<EmployeeReportingMappingEntity> mappings = employeeReportingMappingRepository.findByHodUserId(hodUserId);
        if (mappings.isEmpty()) {
            return List.of();
        }
        List<Long> employeeIds = mappings.stream()
                .map(EmployeeReportingMappingEntity::getEmployeeId)
                .collect(Collectors.toList());
        
        List<LeaveApplicationEntity> leaves = leaveApplicationRepository.findByEmployeeIdInAndStatusOrderByApplicationDateDesc(employeeIds, "PENDING");
        
        if (leaves.isEmpty()) {
            return List.of();
        }

        Map<Long, EmployeeEntity> employeeMap = employeeRepository.findAllById(employeeIds).stream()
                .collect(Collectors.toMap(EmployeeEntity::getEmployeeId, emp -> emp));

        List<LeaveApplicationHODDTO> dtos = new ArrayList<>();
        for (LeaveApplicationEntity leave : leaves) {
            EmployeeEntity emp = employeeMap.get(leave.getEmployeeId());
            LeaveApplicationHODDTO dto = new LeaveApplicationHODDTO();
            dto.setLeaveId(leave.getLeaveId());
            dto.setEmployeeId(leave.getEmployeeId());
            dto.setEmployeeCode(emp != null ? emp.getEmployeeCode() : "");
            dto.setEmployeeName(emp != null ? emp.getFullName() : "Unknown");
            dto.setLeaveType(leave.getLeaveType());
            dto.setLeaveCategory(leave.getLeaveCategory());
            dto.setStartDate(leave.getStartDate());
            dto.setEndDate(leave.getEndDate());
            dto.setDescription(leave.getDescription());
            dto.setApplicationDate(leave.getApplicationDate());
            dto.setStatus(leave.getStatus());
            dtos.add(dto);
        }
        return dtos;
    }

    @Override
    public void updateLeaveStatus(Long leaveId, String status, String remarks) {
        LeaveApplicationEntity leave = leaveApplicationRepository.findById(leaveId).orElse(null);
        if (leave != null) {
            leave.setStatus(status);
            leave.setHodRemarks(remarks);
            leaveApplicationRepository.save(leave);
        }
    }
}
