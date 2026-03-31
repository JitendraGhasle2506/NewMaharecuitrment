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
    public List<LeaveApplicationHODDTO> getPendingLeavesForHOD(Long hodUserId, String search) {
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

        return convertToHODDTO(leaves, employeeIds, search);
    }

    @Override
    public List<LeaveApplicationHODDTO> getProcessedLeavesForHOD(Long hodUserId, String search) {
        List<EmployeeReportingMappingEntity> mappings = employeeReportingMappingRepository.findByHodUserId(hodUserId);
        if (mappings.isEmpty()) {
            return List.of();
        }
        List<Long> employeeIds = mappings.stream()
                .map(EmployeeReportingMappingEntity::getEmployeeId)
                .collect(Collectors.toList());
        
        List<LeaveApplicationEntity> leaves = leaveApplicationRepository.findByEmployeeIdInAndStatusInOrderByApplicationDateDesc(employeeIds, List.of("APPROVED", "REJECTED"));
        
        if (leaves.isEmpty()) {
            return List.of();
        }

        return convertToHODDTO(leaves, employeeIds, search);
    }

    private List<LeaveApplicationHODDTO> convertToHODDTO(List<LeaveApplicationEntity> leaves, List<Long> employeeIds, String search) {
        Map<Long, EmployeeEntity> employeeMap = employeeRepository.findAllById(employeeIds).stream()
                .collect(Collectors.toMap(EmployeeEntity::getEmployeeId, emp -> emp));

        List<LeaveApplicationHODDTO> dtos = new ArrayList<>();
        for (LeaveApplicationEntity leave : leaves) {
            EmployeeEntity emp = employeeMap.get(leave.getEmployeeId());
            
            // Filter by search query (Name or Designation)
            if (search != null && !search.trim().isEmpty()) {
                String query = search.toLowerCase().trim();
                boolean matchesName = emp != null && emp.getFullName() != null && emp.getFullName().toLowerCase().contains(query);
                
                String designationName = (emp != null && emp.getDesignation() != null) ? emp.getDesignation().getDesignationName() : null;
                boolean matchesDesignation = designationName != null && designationName.toLowerCase().contains(query);
                
                if (!matchesName && !matchesDesignation) {
                    continue;
                }
            }

            LeaveApplicationHODDTO dto = new LeaveApplicationHODDTO();
            dto.setLeaveId(leave.getLeaveId());
            dto.setEmployeeId(leave.getEmployeeId());
            dto.setEmployeeCode(emp != null ? emp.getEmployeeCode() : "");
            dto.setEmployeeName(emp != null ? emp.getFullName() : "Unknown");
            
            String designationName = (emp != null && emp.getDesignation() != null) ? emp.getDesignation().getDesignationName() : "";
            dto.setDesignation(designationName);
            
            dto.setLeaveType(leave.getLeaveType());
            dto.setLeaveCategory(leave.getLeaveCategory());
            dto.setStartDate(leave.getStartDate());
            dto.setEndDate(leave.getEndDate());
            dto.setDescription(leave.getDescription());
            dto.setApplicationDate(leave.getApplicationDate());
            dto.setStatus(leave.getStatus());
            dto.setHodRemarks(leave.getHodRemarks());
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
