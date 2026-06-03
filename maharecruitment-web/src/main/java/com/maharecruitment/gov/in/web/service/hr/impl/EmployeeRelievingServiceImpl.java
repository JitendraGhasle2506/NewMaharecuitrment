package com.maharecruitment.gov.in.web.service.hr.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeRelievingEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRelievingRepository;
import com.maharecruitment.gov.in.recruitment.dto.employee.EmployeeRelievingDto;
import com.maharecruitment.gov.in.web.service.hr.EmployeeRelievingService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeRelievingServiceImpl implements EmployeeRelievingService {
    private final EmployeeRelievingRepository relievingRepository;
    private final EmployeeRepository employeeRepository;

    public EmployeeRelievingServiceImpl(EmployeeRelievingRepository relievingRepository, EmployeeRepository employeeRepository) {
        this.relievingRepository = relievingRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<EmployeeRelievingDto> getAllRelievingRecords() {
        return relievingRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public List<EmployeeRelievingDto> getRelievingRecordsByAgency(Long agencyId) {
        return relievingRepository.findByEmployee_Agency_AgencyId(agencyId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public List<EmployeeRelievingDto> getRelievingRecordsByDepartment(Long departmentId) {
        return relievingRepository.findByEmployee_DepartmentRegistration_DepartmentRegistrationId(departmentId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public EmployeeRelievingDto getRelievingById(Long relievingId) {
        return relievingRepository.findById(relievingId).map(this::mapToDto).orElse(null);
    }

    @Override
    @Transactional
    public void saveRelieving(EmployeeRelievingDto dto) {
        EmployeeRelievingEntity entity;
        if (dto.getRelievingId() != null) {
            entity = relievingRepository.findById(dto.getRelievingId())
                .orElse(new EmployeeRelievingEntity());
        } else {
            List<EmployeeRelievingEntity> existingRecords = relievingRepository.findByEmployee_EmployeeId(dto.getEmployeeId());
            boolean hasActiveRecord = existingRecords.stream()
                .anyMatch(record -> record.getStatus() != null && !record.getStatus().equalsIgnoreCase("Cancelled"));
            if (hasActiveRecord) {
                throw new IllegalArgumentException("This employee already has an active relieving process.");
            }
            entity = new EmployeeRelievingEntity();
        }

        EmployeeEntity employee = employeeRepository.findById(dto.getEmployeeId())
            .orElseThrow(() -> new IllegalArgumentException("Invalid Employee ID"));
        entity.setEmployee(employee);

        entity.setReasonOfRelieving(dto.getReasonOfRelieving());
        entity.setExitDate(dto.getExitDate());
        entity.setResignDate(dto.getResignDate());
        entity.setPipStartDate(dto.getPipStartDate());
        entity.setPipDuration(dto.getPipDuration());
        entity.setHandoverGivenToId(dto.getHandoverGivenToId());
        entity.setStatus(dto.getStatus() == null ? "INITIATED" : dto.getStatus());
        entity.setRemarks(dto.getRemarks());

        relievingRepository.save(entity);
        
        // If status is completed or exit date is passed, we can optionally update employee status to INACTIVE.
        if ("COMPLETED".equalsIgnoreCase(dto.getStatus()) || "Relieved".equalsIgnoreCase(dto.getStatus())) {
            employee.setStatus("INACTIVE");
            employeeRepository.save(employee);
        }
    }

    @Override
    @Transactional
    public void markExitDate(Long relievingId, java.time.LocalDate exitDate) {
        EmployeeRelievingEntity entity = relievingRepository.findById(relievingId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid relieving ID"));
        entity.setExitDate(exitDate);
        entity.setStatus("Exit Date Marked");
        relievingRepository.save(entity);
    }

    @Override
    @Transactional
    public void cancelResignation(Long relievingId) {
        EmployeeRelievingEntity entity = relievingRepository.findById(relievingId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid relieving ID"));
            
        if ("COMPLETED".equalsIgnoreCase(entity.getStatus()) || "Relieved".equalsIgnoreCase(entity.getStatus())) {
            throw new IllegalStateException("Cannot cancel an already completed relieving record.");
        }
        
        entity.setStatus("Cancelled");
        relievingRepository.save(entity);
        
        EmployeeEntity employee = entity.getEmployee();
        if (employee != null) {
            employee.setStatus("ACTIVE");
            employeeRepository.save(employee);
        }
    }

    @Override
    @Transactional
    public void updatePip(Long relievingId, java.time.LocalDate pipStartDate, String pipDuration) {
        EmployeeRelievingEntity entity = relievingRepository.findById(relievingId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid relieving ID"));
        
        if (!"PIP".equalsIgnoreCase(entity.getReasonOfRelieving())) {
            throw new IllegalArgumentException("This record is not a PIP record.");
        }
        
        entity.setPipStartDate(pipStartDate);
        entity.setPipDuration(pipDuration);
        relievingRepository.save(entity);
    }

    private EmployeeRelievingDto mapToDto(EmployeeRelievingEntity entity) {
        EmployeeRelievingDto dto = new EmployeeRelievingDto();
        dto.setRelievingId(entity.getRelievingId());
        if (entity.getEmployee() != null) {
            dto.setEmployeeId(entity.getEmployee().getEmployeeId());
            dto.setEmployeeName(entity.getEmployee().getFullName());
            dto.setEmployeeCode(entity.getEmployee().getEmployeeCode());
            EmployeeEntity emp = entity.getEmployee();
            String agencyName = (emp.getAgency() != null) ? emp.getAgency().getAgencyName() : "N/A";
            String projectName = "N/A";
            if (emp.getPreOnboarding() != null
                    && emp.getPreOnboarding().getInterviewDetail() != null
                    && emp.getPreOnboarding().getInterviewDetail().getRecruitmentNotification() != null
                    && emp.getPreOnboarding().getInterviewDetail().getRecruitmentNotification().getProjectMst() != null
                    && org.springframework.util.StringUtils.hasText(emp.getPreOnboarding().getInterviewDetail().getRecruitmentNotification()
                            .getProjectMst().getProjectName())) {
                projectName = emp.getPreOnboarding().getInterviewDetail().getRecruitmentNotification().getProjectMst()
                        .getProjectName();
            }
            dto.setCompanyName(agencyName);
            dto.setProjectName(projectName);
        }
        dto.setReasonOfRelieving(entity.getReasonOfRelieving());
        dto.setExitDate(entity.getExitDate());
        dto.setResignDate(entity.getResignDate());
        dto.setPipStartDate(entity.getPipStartDate());
        dto.setPipDuration(entity.getPipDuration());
        dto.setHandoverGivenToId(entity.getHandoverGivenToId());
        
        if (entity.getHandoverGivenToId() != null) {
            employeeRepository.findById(entity.getHandoverGivenToId())
                .ifPresent(e -> dto.setHandoverGivenToName(e.getFullName()));
        }
        
        dto.setStatus(entity.getStatus());
        dto.setRemarks(entity.getRemarks());
        return dto;
    }
}
