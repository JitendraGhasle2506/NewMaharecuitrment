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
        entity.setHandoverGivenToId(dto.getHandoverGivenToId());
        entity.setStatus(dto.getStatus() == null ? "INITIATED" : dto.getStatus());
        entity.setRemarks(dto.getRemarks());

        relievingRepository.save(entity);
        
        // If status is completed or exit date is passed, we can optionally update employee status to INACTIVE.
        if ("COMPLETED".equalsIgnoreCase(dto.getStatus())) {
            employee.setStatus("INACTIVE");
            employeeRepository.save(employee);
        }
    }

    private EmployeeRelievingDto mapToDto(EmployeeRelievingEntity entity) {
        EmployeeRelievingDto dto = new EmployeeRelievingDto();
        dto.setRelievingId(entity.getRelievingId());
        if (entity.getEmployee() != null) {
            dto.setEmployeeId(entity.getEmployee().getEmployeeId());
            dto.setEmployeeName(entity.getEmployee().getFullName());
        }
        dto.setReasonOfRelieving(entity.getReasonOfRelieving());
        dto.setExitDate(entity.getExitDate());
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
