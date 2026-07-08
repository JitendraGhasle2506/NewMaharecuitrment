package com.maharecruitment.gov.in.web.service.employee.impl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.dto.employee.ManagerTaskApprovalDto;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeTaskLogEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeTaskLogRepository;
import com.maharecruitment.gov.in.web.service.employee.ManagerTaskService;

@Service
public class ManagerTaskServiceImpl implements ManagerTaskService {

    private final EmployeeTaskLogRepository taskLogRepository;
    private final EmployeeReportingMappingRepository reportingRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public ManagerTaskServiceImpl(EmployeeTaskLogRepository taskLogRepository, 
                                  EmployeeReportingMappingRepository reportingRepository,
                                  EmployeeRepository employeeRepository,
                                  UserRepository userRepository) {
        this.taskLogRepository = taskLogRepository;
        this.reportingRepository = reportingRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<ManagerTaskApprovalDto> getPendingTasksForManager(String loginEmail) {
        EmployeeEntity manager = resolveEmployee(loginEmail);
        if (manager == null) {
            return Collections.emptyList();
        }

        // Find employees reporting to this manager (Assuming managerEmployeeId matches)
        List<EmployeeReportingMappingEntity> reports = reportingRepository.findByManagerEmployeeId(manager.getEmployeeId());
        
        List<Long> employeeIds = reports.stream()
                .map(EmployeeReportingMappingEntity::getEmployeeId)
                .collect(Collectors.toList());

        if (employeeIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<EmployeeTaskLogEntity> pendingTasks = taskLogRepository.findByEmployee_EmployeeIdInAndStatusOrderByTaskDateAsc(employeeIds, "PENDING_APPROVAL");

        return pendingTasks.stream().map(entity -> {
            ManagerTaskApprovalDto dto = new ManagerTaskApprovalDto();
            dto.setTaskId(entity.getId());
            dto.setEmployeeName(entity.getEmployee().getFullName());
            dto.setProjectName(entity.getProjectName());
            dto.setModuleName(entity.getModuleName());
            dto.setTaskDescription(entity.getTaskDescription());
            dto.setTaskDate(entity.getTaskDate());
            dto.setHours(entity.getHoursSpent());
            dto.setStatus(entity.getStatus());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void approveTask(Long taskId, String loginEmail) {
        EmployeeEntity manager = requireEmployee(loginEmail);
        EmployeeTaskLogEntity task = taskLogRepository.findById(taskId).orElseThrow();
        
        task.setStatus("APPROVED");
        task.setApprovedBy(manager.getEmployeeId());
        task.setApprovalDate(LocalDate.now());
        taskLogRepository.save(task);
    }

    @Override
    @Transactional
    public void rejectTask(Long taskId, String remarks, String loginEmail) {
        EmployeeEntity manager = requireEmployee(loginEmail);
        EmployeeTaskLogEntity task = taskLogRepository.findById(taskId).orElseThrow();
        
        task.setStatus("REJECTED");
        task.setApprovedBy(manager.getEmployeeId());
        task.setApprovalDate(LocalDate.now());
        task.setManagerRemarks(remarks);
        taskLogRepository.save(task);
    }

    private EmployeeEntity requireEmployee(String loginEmail) {
        EmployeeEntity employee = resolveEmployee(loginEmail);
        if (employee == null) {
            throw new java.util.NoSuchElementException("Employee not found");
        }
        return employee;
    }

    private EmployeeEntity resolveEmployee(String loginEmail) {
        return userRepository.findByEmailIgnoreCase(loginEmail)
                .flatMap(user -> employeeRepository.findByUser_Id(user.getId()))
                .orElse(null);
    }
}
