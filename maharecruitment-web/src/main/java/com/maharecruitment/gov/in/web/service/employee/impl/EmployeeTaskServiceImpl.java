package com.maharecruitment.gov.in.web.service.employee.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.dto.employee.EmployeeTaskLogDto;
import com.maharecruitment.gov.in.recruitment.dto.employee.TaskSubmissionForm;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeTaskLogEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.attendance.entity.DailyAttendanceInternalEntity;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeTaskLogRepository;
import com.maharecruitment.gov.in.web.service.employee.EmployeeTaskService;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class EmployeeTaskServiceImpl implements EmployeeTaskService {

    private final EmployeeTaskLogRepository employeeTaskLogRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final DailyAttendanceInternalRepository dailyAttendanceInternalRepository;

    public EmployeeTaskServiceImpl(EmployeeTaskLogRepository employeeTaskLogRepository,
                                   EmployeeRepository employeeRepository,
                                   UserRepository userRepository,
                                   DailyAttendanceInternalRepository dailyAttendanceInternalRepository) {
        this.employeeTaskLogRepository = employeeTaskLogRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.dailyAttendanceInternalRepository = dailyAttendanceInternalRepository;
    }

    @Override
    @Transactional
    public void saveTasks(TaskSubmissionForm taskForm, String loginEmail) {
        EmployeeEntity employee = requireEmployee(loginEmail);

        if (taskForm != null && taskForm.getTaskList() != null) {
            for (EmployeeTaskLogDto dto : taskForm.getTaskList()) {
                if (dto.isSelected() && dto.getTaskDescription() != null && !dto.getTaskDescription().trim().isEmpty()) {
                    EmployeeTaskLogEntity entity;
                    if (dto.getTaskId() != null) {
                        entity = employeeTaskLogRepository.findById(dto.getTaskId())
                            .orElseThrow(() -> new RuntimeException("Task not found"));
                        if (!entity.getEmployee().getEmployeeId().equals(employee.getEmployeeId())) {
                            throw new RuntimeException("Unauthorized edit");
                        }
                        if ("APPROVED".equalsIgnoreCase(entity.getStatus())) {
                            throw new RuntimeException("Cannot edit an approved task");
                        }
                    } else {
                        entity = new EmployeeTaskLogEntity();
                        entity.setEmployee(employee);
                    }
                    entity.setStatus("PENDING_APPROVAL");
                    entity.setProjectName(dto.getProjectName());
                    entity.setOtherProjectReason(dto.getOtherProjectReason());
                    entity.setModuleName(dto.getModuleName());
                    entity.setTaskDescription(dto.getTaskDescription());
                    entity.setTaskDate(dto.getTaskDate());
                    entity.setStartTime(dto.getStartTime());
                    entity.setEndTime(dto.getEndTime());
                    entity.setHoursSpent(dto.getHours());
                    entity.setPayableStatus(dto.getPayableStatus());
                    entity.setInTime(dto.getInTime());
                    employeeTaskLogRepository.save(entity);
                }
            }
        }
    }

    @Override
    public Page<EmployeeTaskLogDto> getRecentTasks(String loginEmail, Integer month, Integer year, Pageable pageable) {
        EmployeeEntity employee = requireEmployee(loginEmail);

        Page<EmployeeTaskLogEntity> entityPage;
        if (month != null && year != null) {
            entityPage = employeeTaskLogRepository.findByEmployeeIdAndMonthAndYear(
                    employee.getEmployeeId(), month, year, pageable);
        } else {
            entityPage = employeeTaskLogRepository.findByEmployee_EmployeeIdOrderByTaskDateDesc(
                    employee.getEmployeeId(), pageable);
        }

        List<EmployeeTaskLogDto> dtos = entityPage.getContent().stream().map(entity -> {
            EmployeeTaskLogDto dto = new EmployeeTaskLogDto();
            dto.setTaskId(entity.getId());
            dto.setProjectName(entity.getProjectName());
            dto.setOtherProjectReason(entity.getOtherProjectReason());
            dto.setModuleName(entity.getModuleName());
            dto.setTaskDescription(entity.getTaskDescription());
            dto.setTaskDate(entity.getTaskDate());
            dto.setStartTime(entity.getStartTime());
            dto.setEndTime(entity.getEndTime());
            dto.setHours(entity.getHoursSpent());
            dto.setPayableStatus(entity.getPayableStatus());
            dto.setInTime(entity.getInTime());
            dto.setStatus(entity.getStatus());
            dto.setManagerRemarks(entity.getManagerRemarks());
            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, entityPage.getTotalElements());
    }

    @Override
    public String fetchInTime(String loginEmail, String dateString) {
        EmployeeEntity employee = resolveEmployee(loginEmail).orElse(null);
        if (employee != null && dateString != null && !dateString.isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(dateString);
                Optional<DailyAttendanceInternalEntity> attendanceOpt = dailyAttendanceInternalRepository
                        .findByEmployeeIdAndAttendanceDate(employee.getEmployeeId(), date);
                if (attendanceOpt.isPresent() && attendanceOpt.get().getInTime() != null) {
                    return attendanceOpt.get().getInTime();
                }
            } catch (Exception e) {
                // Return null if parsing fails or error occurs
            }
        }
        return null;
    }

    private EmployeeEntity requireEmployee(String loginEmail) {
        return resolveEmployee(loginEmail)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }

    private Optional<EmployeeEntity> resolveEmployee(String loginEmail) {
        return userRepository.findByEmailIgnoreCase(loginEmail)
                .flatMap(user -> employeeRepository.findByUser_Id(user.getId()));
    }
}
