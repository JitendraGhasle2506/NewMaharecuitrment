package com.maharecruitment.gov.in.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.attendance.dto.TourApplicationHODDTO;
import com.maharecruitment.gov.in.attendance.entity.TourApplicationEntity;
import com.maharecruitment.gov.in.attendance.repository.TourApplicationRepository;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.service.ReportingManagerService;

@ExtendWith(MockitoExtension.class)
class TourApplicationServiceImplTest {

    @Mock
    private TourApplicationRepository tourApplicationRepository;

    @Mock
    private ReportingManagerService reportingManagerService;

    @Mock
    private EmployeeRepository employeeRepository;

    private TourApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TourApplicationServiceImpl(
                tourApplicationRepository,
                reportingManagerService,
                employeeRepository);
    }

    @Test
    void pendingToursUseEffectiveReportingAuthorityEmployees() {
        List<Long> employeeIds = List.of(101L);
        TourApplicationEntity tour = tour(501L, 101L, "PENDING");
        EmployeeEntity employee = employee(101L, "EMP101", "Asha Patil", "Developer");

        when(reportingManagerService.getEffectiveEmployeeIdsForAuthority(7L)).thenReturn(employeeIds);
        when(tourApplicationRepository.findByEmployeeIdInAndStatusOrderByApplicationDateDesc(
                employeeIds,
                "PENDING")).thenReturn(List.of(tour));
        when(employeeRepository.findByEmployeeIdInOrderByFullNameAscEmployeeIdAsc(employeeIds))
                .thenReturn(List.of(employee));

        List<TourApplicationHODDTO> result = service.getPendingToursForHOD(7L, null);

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.getTourId()).isEqualTo(501L);
            assertThat(dto.getEmployeeId()).isEqualTo(101L);
            assertThat(dto.getEmployeeName()).isEqualTo("Asha Patil");
            assertThat(dto.getDesignation()).isEqualTo("Developer");
        });
        verify(reportingManagerService).getEffectiveEmployeeIdsForAuthority(7L);
    }

    @Test
    void processedToursUseEffectiveReportingAuthorityEmployees() {
        List<Long> employeeIds = List.of(101L);
        List<String> statuses = List.of("APPROVED", "REJECTED");
        TourApplicationEntity tour = tour(502L, 101L, "APPROVED");
        EmployeeEntity employee = employee(101L, "EMP101", "Asha Patil", "Developer");

        when(reportingManagerService.getEffectiveEmployeeIdsForAuthority(7L)).thenReturn(employeeIds);
        when(tourApplicationRepository.findByEmployeeIdInAndStatusInOrderByApplicationDateDesc(
                employeeIds,
                statuses)).thenReturn(List.of(tour));
        when(employeeRepository.findByEmployeeIdInOrderByFullNameAscEmployeeIdAsc(employeeIds))
                .thenReturn(List.of(employee));

        List<TourApplicationHODDTO> result = service.getProcessedToursForHOD(7L, null);

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.getTourId()).isEqualTo(502L);
            assertThat(dto.getStatus()).isEqualTo("APPROVED");
        });
        verify(reportingManagerService).getEffectiveEmployeeIdsForAuthority(7L);
    }

    private TourApplicationEntity tour(Long tourId, Long employeeId, String status) {
        TourApplicationEntity tour = new TourApplicationEntity();
        tour.setTourId(tourId);
        tour.setEmployeeId(employeeId);
        tour.setTourCategory("Full Day");
        tour.setStartDate(LocalDate.of(2026, 9, 5));
        tour.setEndDate(LocalDate.of(2026, 9, 6));
        tour.setApplicationDate(LocalDateTime.of(2026, 9, 2, 10, 30));
        tour.setStatus(status);
        return tour;
    }

    private EmployeeEntity employee(Long employeeId, String code, String name, String designationName) {
        ManpowerDesignationMaster designation = new ManpowerDesignationMaster();
        designation.setDesignationName(designationName);

        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeCode(code);
        employee.setFullName(name);
        employee.setDesignation(designation);
        return employee;
    }
}
