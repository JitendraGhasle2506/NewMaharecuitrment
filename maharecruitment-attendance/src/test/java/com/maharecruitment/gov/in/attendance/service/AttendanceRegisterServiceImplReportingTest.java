package com.maharecruitment.gov.in.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.attendance.dto.AttendanceRegisterDTO;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.attendance.repository.HolidayRepository;
import com.maharecruitment.gov.in.attendance.repository.LeaveApplicationRepository;
import com.maharecruitment.gov.in.attendance.repository.ManualAttendanceRequestRepository;
import com.maharecruitment.gov.in.attendance.repository.TourApplicationRepository;
import com.maharecruitment.gov.in.attendance.repository.WeekOffWorkingDayRepository;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.recruitment.entity.CellReportingAuthorityMappingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeCellMappingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;
import com.maharecruitment.gov.in.recruitment.repository.CellReportingAuthorityMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeCellMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeLocationMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceRegisterServiceImplReportingTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmployeeReportingMappingRepository employeeReportingMappingRepository;
    @Mock private EmployeeCellMappingRepository employeeCellMappingRepository;
    @Mock private CellReportingAuthorityMappingRepository cellReportingAuthorityMappingRepository;
    @Mock private EmployeeLocationMappingRepository employeeLocationMappingRepository;
    @Mock private DailyAttendanceInternalRepository dailyAttendanceInternalRepository;
    @Mock private HolidayRepository holidayRepository;
    @Mock private WeekOffWorkingDayRepository weekOffWorkingDayRepository;
    @Mock private LeaveApplicationRepository leaveApplicationRepository;
    @Mock private TourApplicationRepository tourApplicationRepository;
    @Mock private ManualAttendanceRequestRepository manualAttendanceRequestRepository;

    @InjectMocks
    private AttendanceRegisterServiceImpl service;

    private EmployeeEntity employee;

    @BeforeEach
    void setUp() {
        employee = new EmployeeEntity();
        employee.setEmployeeId(501L);
        employee.setFullName("Employee");
        when(employeeRepository.findById(501L)).thenReturn(Optional.of(employee));
    }

    @ParameterizedTest
    @ValueSource(strings = {"OTHER", "STM", "PM", "HOD"})
    void selectedManagerNameTakesPrecedenceForEveryManagerType(String managerType) {
        givenIndividualMapping(managerType, 60L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "Reporting Authority")));
        EmployeeEntity manager = new EmployeeEntity();
        manager.setEmployeeId(60L);
        manager.setFullName("Selected Manager");
        when(employeeRepository.findById(60L)).thenReturn(Optional.of(manager));

        AttendanceRegisterDTO dto = attendance();

        assertThat(dto.getReportingHOD()).isEqualTo("Reporting Authority");
        assertThat(dto.getReportingManager()).isEqualTo("Selected Manager");
        verifyNoInteractions(employeeCellMappingRepository, cellReportingAuthorityMappingRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"OTHER", "STM", "PM", "HOD"})
    void directReportUsesAuthorityNameWhenThereIsNoSeparateManager(String managerType) {
        givenIndividualMapping(managerType, null);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "Reporting Authority")));

        AttendanceRegisterDTO dto = attendance();

        assertThat(dto.getReportingHOD()).isEqualTo("Reporting Authority");
        assertThat(dto.getReportingManager()).isEqualTo("Reporting Authority");
        verify(userRepository).findById(7L);
        verifyNoInteractions(employeeCellMappingRepository, cellReportingAuthorityMappingRepository);
    }

    @Test
    void usesCellReportingAuthorityWhenThereIsNoIndividualMapping() {
        givenCellAuthority(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "Cell Authority")));

        AttendanceRegisterDTO dto = attendance();

        assertThat(dto.getReportingHOD()).isEqualTo("Cell Authority");
        assertThat(dto.getReportingManager()).isEqualTo("Cell Authority");
        verify(userRepository).findById(7L);
    }

    @Test
    void cellAuthorityDoesNotAppearAsTheirOwnReportingManager() {
        employee.setUser(user(7L, "Cell Authority"));
        givenCellAuthority(7L);

        AttendanceRegisterDTO dto = attendance();

        assertThat(dto.getReportingHOD()).isNull();
        assertThat(dto.getReportingManager()).isNull();
        verifyNoInteractions(userRepository);
    }

    @Test
    void missingReportingAssignmentsLeaveNamesEmpty() {
        AttendanceRegisterDTO dto = attendance();

        assertThat(dto.getReportingHOD()).isNull();
        assertThat(dto.getReportingManager()).isNull();
        verifyNoInteractions(cellReportingAuthorityMappingRepository, userRepository);
    }

    @Test
    void missingSelectedManagerDoesNotSubstituteHodName() {
        givenIndividualMapping("OTHER", 60L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "Reporting Authority")));

        AttendanceRegisterDTO dto = attendance();

        assertThat(dto.getReportingHOD()).isEqualTo("Reporting Authority");
        assertThat(dto.getReportingManager()).isNull();
        verifyNoInteractions(employeeCellMappingRepository, cellReportingAuthorityMappingRepository);
    }

    @Test
    void missingAuthorityDoesNotHideSelectedManagerOrOverrideIndividualAssignment() {
        givenIndividualMapping("OTHER", 60L);
        EmployeeEntity manager = new EmployeeEntity();
        manager.setEmployeeId(60L);
        manager.setFullName("Selected Manager");
        when(employeeRepository.findById(60L)).thenReturn(Optional.of(manager));

        AttendanceRegisterDTO dto = attendance();

        assertThat(dto.getReportingHOD()).isNull();
        assertThat(dto.getReportingManager()).isEqualTo("Selected Manager");
        verifyNoInteractions(employeeCellMappingRepository, cellReportingAuthorityMappingRepository);
    }

    private AttendanceRegisterDTO attendance() {
        return service.getInternalAttendanceForEmployee(501L, 9, 2026);
    }

    private void givenIndividualMapping(String managerType, Long managerEmployeeId) {
        EmployeeReportingMappingEntity mapping = new EmployeeReportingMappingEntity();
        mapping.setMappingId(90L);
        mapping.setEmployeeId(501L);
        mapping.setHodUserId(7L);
        mapping.setManagerType(managerType);
        mapping.setManagerEmployeeId(managerEmployeeId);
        when(employeeReportingMappingRepository.findFirstByEmployeeIdOrderByMappingIdDesc(501L))
                .thenReturn(Optional.of(mapping));
    }

    private void givenCellAuthority(Long userId) {
        CellMaster cell = CellMaster.builder().cellId(11L).cellName("Applications").build();
        EmployeeCellMappingEntity cellMapping = new EmployeeCellMappingEntity();
        cellMapping.setEmployee(employee);
        cellMapping.setCell(cell);
        when(employeeCellMappingRepository.findByEmployeeEmployeeId(501L)).thenReturn(Optional.of(cellMapping));
        CellReportingAuthorityMappingEntity authorityMapping = new CellReportingAuthorityMappingEntity();
        authorityMapping.setCell(cell);
        authorityMapping.setAuthorityUserId(userId);
        when(cellReportingAuthorityMappingRepository.findByCellCellId(11L)).thenReturn(Optional.of(authorityMapping));
    }

    private User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        return user;
    }
}
