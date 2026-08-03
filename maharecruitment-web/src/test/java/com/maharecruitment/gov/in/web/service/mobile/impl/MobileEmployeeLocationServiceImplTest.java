package com.maharecruitment.gov.in.web.service.mobile.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.master.entity.LocationMaster;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeLocationMappingEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeLocationMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.dto.mobile.MobileEmployeeLocationResponse;
import com.maharecruitment.gov.in.web.service.mobile.MobileApiException;
import com.maharecruitment.gov.in.web.service.mobile.MobileEmployeeAccessService;

@ExtendWith(MockitoExtension.class)
class MobileEmployeeLocationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeLocationMappingRepository employeeLocationMappingRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMappedLocationsReturnsActiveLocationsForLoggedInEmployee() {
        authenticate("employee@example.com");
        EmployeeEntity employee = employee(101L, "EMP101", "employee@example.com");
        LocationMaster location = location(
                11L,
                "  Head Office  ",
                "  Mumbai  ",
                new BigDecimal("19.0760000"),
                new BigDecimal("72.8777000"),
                150,
                "Y");

        when(employeeRepository.findMobileLoginProfileByUserId(10L)).thenReturn(Optional.of(employee));
        when(employeeLocationMappingRepository
                .findByEmployeeEmployeeIdOrderByPrimaryLocationDescLocationLocationNameAsc(101L))
                .thenReturn(List.of(mapping(employee, location, true)));

        MobileEmployeeLocationResponse response = service().getMappedLocations(101L);

        assertThat(response.success()).isTrue();
        assertThat(response.employeeId()).isEqualTo(101L);
        assertThat(response.locations()).hasSize(1);
        MobileEmployeeLocationResponse.Location mappedLocation = response.locations().get(0);
        assertThat(mappedLocation.locationId()).isEqualTo(11L);
        assertThat(mappedLocation.officeName()).isEqualTo("Head Office");
        assertThat(mappedLocation.locationName()).isEqualTo("Mumbai");
        assertThat(mappedLocation.latitude()).isEqualByComparingTo("19.0760000");
        assertThat(mappedLocation.longitude()).isEqualByComparingTo("72.8777000");
        assertThat(mappedLocation.radiusMeters()).isEqualTo(150);
        assertThat(mappedLocation.displayName()).isEqualTo("Head Office - Mumbai");
        assertThat(mappedLocation.primary()).isTrue();
    }

    @Test
    void getMappedLocationsFiltersInactiveLocations() {
        authenticate("employee@example.com");
        EmployeeEntity employee = employee(101L, "EMP101", "employee@example.com");
        LocationMaster activeLocation = location(
                11L,
                "Head Office",
                "Mumbai",
                new BigDecimal("19.0760000"),
                new BigDecimal("72.8777000"),
                150,
                "Y");
        LocationMaster inactiveLocation = location(
                12L,
                "Old Office",
                "Pune",
                new BigDecimal("18.5204000"),
                new BigDecimal("73.8567000"),
                100,
                "N");

        when(employeeRepository.findMobileLoginProfileByUserId(10L)).thenReturn(Optional.of(employee));
        when(employeeLocationMappingRepository
                .findByEmployeeEmployeeIdOrderByPrimaryLocationDescLocationLocationNameAsc(101L))
                .thenReturn(List.of(
                        mapping(employee, inactiveLocation, true),
                        mapping(employee, activeLocation, false)));

        MobileEmployeeLocationResponse response = service().getMappedLocations(101L);

        assertThat(response.locations())
                .extracting(MobileEmployeeLocationResponse.Location::locationId)
                .containsExactly(11L);
        assertThat(response.locations().get(0).primary()).isFalse();
    }

    @Test
    void getMappedLocationsReturnsEmptyListWhenNoMappingExists() {
        authenticate("employee@example.com");
        EmployeeEntity employee = employee(101L, "EMP101", "employee@example.com");
        when(employeeRepository.findMobileLoginProfileByUserId(10L)).thenReturn(Optional.of(employee));
        when(employeeLocationMappingRepository
                .findByEmployeeEmployeeIdOrderByPrimaryLocationDescLocationLocationNameAsc(101L))
                .thenReturn(List.of());

        MobileEmployeeLocationResponse response = service().getMappedLocations(101L);

        assertThat(response.success()).isTrue();
        assertThat(response.locations()).isEmpty();
    }

    @Test
    void getMappedLocationsRejectsAnotherEmployeeId() {
        authenticate("employee@example.com");
        EmployeeEntity employee = employee(101L, "EMP101", "employee@example.com");
        when(employeeRepository.findMobileLoginProfileByUserId(10L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> service().getMappedLocations(999L))
                .isInstanceOfSatisfying(MobileApiException.class, ex -> {
                    assertThat(ex.getStatus().value()).isEqualTo(403);
                    assertThat(ex.getCode()).isEqualTo("EMPLOYEE_MISMATCH");
                });

        verifyNoInteractions(employeeLocationMappingRepository);
    }

    private MobileEmployeeLocationServiceImpl service() {
        return new MobileEmployeeLocationServiceImpl(
                new MobileEmployeeAccessService(userRepository, employeeRepository),
                employeeLocationMappingRepository);
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of()));
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue(username)).thenReturn(Optional.of(user(username)));
    }

    private User user(String email) {
        User user = new User();
        user.setId(10L);
        user.setName("Test Employee");
        user.setEmail(email);
        user.setMobileNo("9876543210");
        user.setActive(true);
        return user;
    }

    private EmployeeEntity employee(Long employeeId, String employeeCode, String email) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeCode(employeeCode);
        employee.setEmail(email);
        employee.setFullName("Test Employee");
        employee.setStatus("ACTIVE");
        return employee;
    }

    private EmployeeLocationMappingEntity mapping(EmployeeEntity employee, LocationMaster location) {
        return mapping(employee, location, false);
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

    private LocationMaster location(
            Long locationId,
            String officeName,
            String locationName,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer radiusMeters,
            String activeFlag) {
        LocationMaster location = new LocationMaster();
        location.setLocationId(locationId);
        location.setOfficeName(officeName);
        location.setLocationName(locationName);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setRadiusMeters(radiusMeters);
        location.setActiveFlag(activeFlag);
        return location;
    }
}
