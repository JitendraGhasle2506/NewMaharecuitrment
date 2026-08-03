package com.maharecruitment.gov.in.web.service.hr.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.master.entity.LocationMaster;
import com.maharecruitment.gov.in.master.repository.LocationMasterRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeLocationMappingAuditLogEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeLocationMappingEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
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
        when(employeeLocationMappingRepository
                .findByEmployeeEmployeeIdOrderByPrimaryLocationDescLocationLocationNameAsc(101L))
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

    @Test
    void employee1049CanLoadOnePrimaryAndMultipleSecondaryLocations() {
        EmployeeEntity employee = employee(1049L);
        LocationMaster primary = location(11L, "Head Office", "Mumbai");
        LocationMaster secondaryOne = location(12L, "Regional Office", "Pune");
        LocationMaster secondaryTwo = location(13L, "", "Nagpur");

        when(employeeRepository.findDetailedByEmployeeId(1049L)).thenReturn(Optional.of(employee));
        when(employeeLocationMappingRepository
                .findByEmployeeEmployeeIdOrderByPrimaryLocationDescLocationLocationNameAsc(1049L))
                .thenReturn(List.of(
                        mapping(employee, primary, true),
                        mapping(employee, secondaryOne, false),
                        mapping(employee, secondaryTwo, false)));
        when(locationMasterRepository.findByActiveFlagIgnoreCaseOrderByLocationNameAsc("Y"))
                .thenReturn(List.of(primary, secondaryOne, secondaryTwo));
        when(auditLogRepository.findTop10ByEmployeeEmployeeIdOrderByOccurredAtDescAuditIdDesc(1049L))
                .thenReturn(List.of());

        EmployeeLocationMappingEditView result = service().loadMapping(1049L);

        assertThat(result.selectedLocations()).hasSize(3);
        assertThat(result.selectedLocations().get(0).primary()).isTrue();
        assertThat(result.selectedLocations())
                .filteredOn(location -> !location.primary())
                .extracting(location -> location.locationId())
                .containsExactly(12L, 13L);
    }

    @Test
    void savingWithoutPrimaryLocationFailsValidation() {
        assertThatThrownBy(() -> service().updateMapping(1049L, List.of(11L), null, "hr-user"))
                .isInstanceOf(RecruitmentNotificationException.class)
                .hasMessageContaining("primary");
    }

    @Test
    void primaryLocationMustBeIncludedInSelectedLocations() {
        EmployeeEntity employee = employee(1049L);
        LocationMaster selected = location(11L, "Head Office", "Mumbai");
        when(employeeRepository.findDetailedByEmployeeId(1049L)).thenReturn(Optional.of(employee));
        when(locationMasterRepository.findAllById(any(Collection.class))).thenReturn(List.of(selected));

        assertThatThrownBy(() -> service().updateMapping(1049L, List.of(11L), 12L, "hr-user"))
                .isInstanceOf(RecruitmentNotificationException.class)
                .hasMessageContaining("included in the selected locations");
    }

    @Test
    void changingPrimaryAndRemovingLocationUsesSafeTransactionalOrder() {
        EmployeeEntity employee = employee(1049L);
        LocationMaster removedPrimary = location(11L, "Head Office", "Mumbai");
        LocationMaster retainedNewPrimary = location(12L, "Regional Office", "Pune");
        LocationMaster addedSecondary = location(13L, "", "Nagpur");
        EmployeeLocationMappingEntity oldPrimaryMapping = mapping(employee, removedPrimary, true);
        oldPrimaryMapping.setEmployeeLocationMappingId(101L);
        EmployeeLocationMappingEntity retainedMapping = mapping(employee, retainedNewPrimary, false);
        retainedMapping.setEmployeeLocationMappingId(102L);

        when(employeeRepository.findDetailedByEmployeeId(1049L)).thenReturn(Optional.of(employee));
        when(locationMasterRepository.findAllById(any(Collection.class)))
                .thenReturn(List.of(retainedNewPrimary, addedSecondary));
        when(employeeLocationMappingRepository
                .findByEmployeeEmployeeIdOrderByPrimaryLocationDescLocationLocationNameAsc(1049L))
                .thenReturn(List.of(oldPrimaryMapping, retainedMapping));

        boolean changed = service().updateMapping(1049L, List.of(12L, 13L), 12L, "hr-user");

        assertThat(changed).isTrue();
        assertThat(retainedMapping.getPrimaryLocation()).isTrue();

        ArgumentCaptor<Iterable<EmployeeLocationMappingEntity>> removedCaptor =
                ArgumentCaptor.forClass(Iterable.class);
        verify(employeeLocationMappingRepository).deleteAll(removedCaptor.capture());
        assertThat(removedCaptor.getValue())
                .extracting(mapping -> mapping.getLocation().getLocationId())
                .containsExactly(11L);

        ArgumentCaptor<List<EmployeeLocationMappingEntity>> addedCaptor = ArgumentCaptor.forClass(List.class);
        verify(employeeLocationMappingRepository).saveAll(addedCaptor.capture());
        assertThat(addedCaptor.getValue())
                .singleElement()
                .satisfies(mapping -> {
                    assertThat(mapping.getLocation().getLocationId()).isEqualTo(13L);
                    assertThat(mapping.getPrimaryLocation()).isFalse();
                });

        InOrder repositoryOrder = inOrder(employeeLocationMappingRepository);
        repositoryOrder.verify(employeeLocationMappingRepository).clearPrimaryForEmployee(1049L);
        repositoryOrder.verify(employeeLocationMappingRepository).deleteAll(any(Iterable.class));
        repositoryOrder.verify(employeeLocationMappingRepository).saveAll(any(List.class));
        repositoryOrder.verify(employeeLocationMappingRepository).flush();
        repositoryOrder.verify(employeeLocationMappingRepository).save(retainedMapping);
        repositoryOrder.verify(employeeLocationMappingRepository).flush();

        ArgumentCaptor<EmployeeLocationMappingAuditLogEntity> auditCaptor =
                ArgumentCaptor.forClass(EmployeeLocationMappingAuditLogEntity.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getRemovedLocationIds()).isEqualTo("11");
        assertThat(auditCaptor.getValue().getAddedLocationIds()).isEqualTo("13");
        assertThat(auditCaptor.getValue().getDetails())
                .contains("Primary changed from: Head Office - Mumbai to: Regional Office - Pune");
    }

    private EmployeeLocationMappingPageServiceImpl service() {
        return new EmployeeLocationMappingPageServiceImpl(
                employeeRepository,
                locationMasterRepository,
                employeeLocationMappingRepository,
                auditLogRepository);
    }

    private EmployeeEntity employee(Long employeeId) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeCode("EMP-" + employeeId);
        employee.setFullName("Employee " + employeeId);
        employee.setStatus("ACTIVE");
        employee.setOnboardingDate(LocalDate.of(2026, 7, 1));
        return employee;
    }

    private EmployeeLocationMappingEntity mapping(
            EmployeeEntity employee,
            LocationMaster location,
            boolean primary) {
        EmployeeLocationMappingEntity mapping = new EmployeeLocationMappingEntity();
        mapping.setEmployee(employee);
        mapping.setLocation(location);
        mapping.setPrimaryLocation(primary);
        return mapping;
    }

    private LocationMaster location(Long locationId, String officeName, String locationName) {
        LocationMaster location = new LocationMaster();
        location.setLocationId(locationId);
        location.setOfficeName(officeName);
        location.setLocationName(locationName);
        location.setLatitude(new BigDecimal("19.0760"));
        location.setLongitude(new BigDecimal("72.8777"));
        location.setRadiusMeters(150);
        location.setActiveFlag("Y");
        return location;
    }
}
