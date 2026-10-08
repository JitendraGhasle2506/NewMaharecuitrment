package com.maharecruitment.gov.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;
import com.maharecruitment.gov.in.master.entity.LocationMaster;
import com.maharecruitment.gov.in.master.entity.SubDepartment;
import com.maharecruitment.gov.in.master.repository.CellMasterRepository;
import com.maharecruitment.gov.in.master.repository.DepartmentMstRepository;
import com.maharecruitment.gov.in.master.repository.LocationMasterRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeTypeConversionAuditEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeTypeConversionAuditRepository;
import com.maharecruitment.gov.in.web.service.hr.EmployeeCellMappingPageService;
import com.maharecruitment.gov.in.web.service.hr.EmployeeLocationMappingPageService;
import com.maharecruitment.gov.in.web.service.hr.EmployeeTypeConversionService.ConversionCommand;
import com.maharecruitment.gov.in.web.service.hr.EmployeeTypeConversionService.ConversionResult;
import com.maharecruitment.gov.in.web.service.hr.impl.EmployeeTypeConversionServiceImpl;

@ExtendWith(MockitoExtension.class)
class EmployeeTypeConversionTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentMstRepository departmentRepository;

    @Mock private LocationMasterRepository locationRepository;

    @Mock private CellMasterRepository cellRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmployeeReportingMappingRepository reportingMappingRepository;
    @Mock private EmployeeTypeConversionAuditRepository auditRepository;
    @Mock private EmployeeCellMappingPageService cellMappingService;
    @Mock private EmployeeLocationMappingPageService locationMappingService;

    private EmployeeTypeConversionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmployeeTypeConversionServiceImpl(
                employeeRepository,
                departmentRepository,
                locationRepository,
                cellRepository,
                userRepository,
                reportingMappingRepository,
                auditRepository,
                cellMappingService,
                locationMappingService);
    }

    @Test
    void convertsInternalEmployeeAndMapsDepartment() {
        EmployeeEntity employee = employee(21L, "INTERNAL");
        DepartmentMst department = department(8L, "Finance");
        LocationMaster location = location(12L, "Mumbai Office");
        when(employeeRepository.findDetailedByEmployeeId(21L)).thenReturn(Optional.of(employee));
        when(departmentRepository.findById(8L)).thenReturn(Optional.of(department));
        when(locationRepository.findByLocationId(12L)).thenReturn(Optional.of(location));

        ConversionResult result = service.convert(command(21L, "EXTERNAL", 8L, 12L));

        assertThat(employee.getRecruitmentType()).isEqualTo("EXTERNAL");
        assertThat(employee.getDepartment()).isSameAs(department);
        assertThat(result.sourceType()).isEqualTo("INTERNAL");
        verify(employeeRepository).saveAndFlush(employee);
        verify(locationMappingService).updateMapping(21L, List.of(12L), 12L, "hr@test.in");
    }

    @Test
    void clearsIncompatibleSubDepartment() {
        EmployeeEntity employee = employee(22L, "EXTERNAL");
        SubDepartment oldSubDepartment = new SubDepartment();
        oldSubDepartment.setDepartment(department(3L, "Technology"));
        employee.setSubDepartment(oldSubDepartment);
        when(employeeRepository.findDetailedByEmployeeId(22L)).thenReturn(Optional.of(employee));

        CellMaster cell = new CellMaster();
        cell.setCellId(4L);
        cell.setCellName("Applications");
        User hod = new User();
        hod.setId(31L);
        hod.setName("Reporting HOD");
        hod.setActive(true);
        EmployeeEntity hodEmployee = employee(46L, "MAHAIT");
        EmployeeEntity manager = employee(45L, "INTERNAL");
        LocationMaster location = location(13L, "Pune Office");
        when(cellRepository.findByCellId(4L)).thenReturn(Optional.of(cell));
        when(userRepository.findById(31L)).thenReturn(Optional.of(hod));
        when(employeeRepository.findByUser_Id(31L)).thenReturn(Optional.of(hodEmployee));
        when(employeeRepository.findById(45L)).thenReturn(Optional.of(manager));
        when(locationRepository.findByLocationId(13L)).thenReturn(Optional.of(location));

        service.convert(new ConversionCommand(22L, "INTERNAL", null, 13L, 4L, 31L, "PM", 45L, "hr@test.in"));

        assertThat(employee.getSubDepartment()).isNull();
        assertThat(employee.getDepartment()).isNull();
        verify(cellMappingService).updateMapping(22L, 4L, "hr@test.in");
        verify(locationMappingService).updateMapping(22L, List.of(13L), 13L, "hr@test.in");
        verify(reportingMappingRepository).save(any());
        ArgumentCaptor<EmployeeTypeConversionAuditEntity> auditCaptor =
                ArgumentCaptor.forClass(EmployeeTypeConversionAuditEntity.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getDepartmentId()).isZero();
        assertThat(auditCaptor.getValue().getDepartmentName()).isEqualTo("Not mapped");
    }

    @Test
    void rejectsSameTypeConversion() {
        EmployeeEntity employee = employee(23L, "INTERNAL");
        when(employeeRepository.findDetailedByEmployeeId(23L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> service.convert(command(23L, "INTERNAL", 4L, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already an internal employee");
    }

    @Test
    void templateContainsBothDirectionsAndDepartmentMapping() throws IOException {
        String template = new ClassPathResource("templates/hr/employee-type-conversion.html")
                .getContentAsString(StandardCharsets.UTF_8);
        String script = new ClassPathResource("static/js/employee-type-conversion.js")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("Internal to External", "External to Internal", "Map department", "Conversion summary");
        assertThat(script)
                .contains("change.employeeTypeConversion", "bindSelectChange(employeeSelect, updateEmployeeDetails)");
    }

    private ConversionCommand command(Long employeeId, String targetType, Long departmentId, Long locationId) {
        return new ConversionCommand(
                employeeId, targetType, departmentId, locationId, null, null, null, null, "hr@test.in");
    }

    private EmployeeEntity employee(Long id, String type) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(id);
        employee.setFullName("Aarav Patil");
        employee.setRecruitmentType(type);
        employee.setStatus("ACTIVE");
        return employee;
    }

    private DepartmentMst department(Long id, String name) {
        DepartmentMst department = new DepartmentMst();
        department.setDepartmentId(id);
        department.setDepartmentName(name);
        return department;
    }

    private LocationMaster location(Long id, String name) {
        LocationMaster location = new LocationMaster();
        location.setLocationId(id);
        location.setLocationName(name);
        location.setActiveFlag("Y");
        return location;
    }
}
