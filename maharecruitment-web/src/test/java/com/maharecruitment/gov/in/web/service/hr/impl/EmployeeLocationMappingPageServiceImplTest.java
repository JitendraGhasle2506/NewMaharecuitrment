package com.maharecruitment.gov.in.web.service.hr.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.master.repository.LocationMasterRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeLocationMappingAuditLogRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeLocationMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeLocationMappingEditView;

@ExtendWith(MockitoExtension.class)
class EmployeeLocationMappingPageServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LocationMasterRepository locationMasterRepository;

    @Mock
    private EmployeeLocationMappingRepository employeeLocationMappingRepository;

    @Mock
    private EmployeeLocationMappingAuditLogRepository auditLogRepository;

    @Test
    void loadMappingAllowsImportedActiveEmployeeWithoutPreOnboardingRecord() {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(101L);
        employee.setEmployeeCode("EMP-101");
        employee.setFullName("Imported Employee");
        employee.setStatus("ACTIVE");
        employee.setOnboardingDate(LocalDate.of(2026, 7, 1));

        when(employeeRepository.findDetailedByEmployeeId(101L)).thenReturn(Optional.of(employee));
        when(employeeLocationMappingRepository.findByEmployeeEmployeeIdOrderByLocationLocationNameAsc(101L))
                .thenReturn(List.of());
        when(locationMasterRepository.findByActiveFlagIgnoreCaseOrderByLocationNameAsc("Y"))
                .thenReturn(List.of());
        when(auditLogRepository.findTop10ByEmployeeEmployeeIdOrderByOccurredAtDescAuditIdDesc(101L))
                .thenReturn(List.of());

        EmployeeLocationMappingEditView result = service().loadMapping(101L);

        assertThat(result.employee().employeeId()).isEqualTo(101L);
        assertThat(result.employee().employeeCode()).isEqualTo("EMP-101");
        assertThat(result.availableLocations()).isEmpty();
    }

    private EmployeeLocationMappingPageServiceImpl service() {
        return new EmployeeLocationMappingPageServiceImpl(
                employeeRepository,
                locationMasterRepository,
                employeeLocationMappingRepository,
                auditLogRepository);
    }
}
