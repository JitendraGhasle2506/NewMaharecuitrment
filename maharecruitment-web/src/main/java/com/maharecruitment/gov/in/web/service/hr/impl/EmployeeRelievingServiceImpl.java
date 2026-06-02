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

    private EmployeeRelievingDto mapToDto(EmployeeRelievingEntity entity) {
        EmployeeRelievingDto dto = new EmployeeRelievingDto();
        dto.setRelievingId(entity.getRelievingId());
        if (entity.getEmployee() != null) {
            dto.setEmployeeId(entity.getEmployee().getEmployeeId());
            dto.setEmployeeName(entity.getEmployee().getFullName());
            dto.setEmployeeCode(entity.getEmployee().getEmployeeCode());
            if (entity.getEmployee().getDepartmentRegistration() != null) {
                dto.setDepartmentName(entity.getEmployee().getDepartmentRegistration().getDepartmentName());
            } else {
                dto.setDepartmentName("N/A");
            }
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
