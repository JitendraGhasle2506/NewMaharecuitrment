package com.maharecruitment.gov.in.web.service.dashboard.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.attendance.repository.AttendanceRegisterRepo;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.master.entity.WingMaster;
import com.maharecruitment.gov.in.master.repository.CellMasterRepository;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.master.repository.WingMasterRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeCellCountProjection;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeCellMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRWingReportView;

@ExtendWith(MockitoExtension.class)
class HRDashboardServiceImplTest {

    @Mock
    private ProjectMstRepository projectMstRepository;

    @Mock
    private WingMasterRepository wingMasterRepository;

    @Mock
    private CellMasterRepository cellMasterRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeCellMappingRepository employeeCellMappingRepository;

    @Mock
    private DepartmentProjectApplicationRepository departmentProjectApplicationRepository;

    @Mock
    private DailyAttendanceInternalRepository dailyAttendanceInternalRepository;

    @Mock
    private AttendanceRegisterRepo attendanceRegisterRepo;

    @Test
    void getWingReportCountsEmployeesFromEmployeeCellMappings() {
        WingMaster wing = wing(2L, "MAHAIT Project Cells");
        CellMaster networkCell = cell(27L, "Network Infra Cell", wing);
        CellMaster fieldCell = cell(30L, "Field Operations Cell", wing);

        when(wingMasterRepository.findByWingIdAndActiveFlagIgnoreCase(2L, "Y"))
                .thenReturn(Optional.of(wing));
        when(cellMasterRepository.findByWing_WingIdAndActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCaseOrderByCellNameAsc(
                2L,
                "Y",
                "Y"))
                .thenReturn(List.of(fieldCell, networkCell));
        when(projectMstRepository.summarizeProjectCountsByCellAndWingId(2L)).thenReturn(List.of());
        when(employeeCellMappingRepository.summarizeActiveEmployeesByCellAndWingId(2L, "Y", "ACTIVE"))
                .thenReturn(List.of(cellCount(27L, 4L), cellCount(30L, 2L)));

        Optional<HRWingReportView> result = service().getWingReport(2L);

        assertThat(result).isPresent();
        HRWingReportView report = result.orElseThrow();
        assertThat(report.employeeCount()).isEqualTo(6);
        assertThat(report.cells())
                .extracting(cell -> cell.cellId(), cell -> cell.employeeCount())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(30L, 2),
                        org.assertj.core.groups.Tuple.tuple(27L, 4));
        verify(employeeCellMappingRepository).summarizeActiveEmployeesByCellAndWingId(2L, "Y", "ACTIVE");
    }

    private HRDashboardServiceImpl service() {
        return new HRDashboardServiceImpl(
                projectMstRepository,
                wingMasterRepository,
                cellMasterRepository,
                employeeRepository,
                employeeCellMappingRepository,
                departmentProjectApplicationRepository,
                dailyAttendanceInternalRepository,
                attendanceRegisterRepo);
    }

    private WingMaster wing(Long wingId, String wingName) {
        WingMaster wing = new WingMaster();
        wing.setWingId(wingId);
        wing.setWingName(wingName);
        wing.setActiveFlag("Y");
        return wing;
    }

    private CellMaster cell(Long cellId, String cellName, WingMaster wing) {
        CellMaster cell = new CellMaster();
        cell.setCellId(cellId);
        cell.setCellName(cellName);
        cell.setActiveFlag("Y");
        cell.setWing(wing);
        return cell;
    }

    private EmployeeCellCountProjection cellCount(Long cellId, Long employeeCount) {
        return new EmployeeCellCountProjection() {

            @Override
            public Long getCellId() {
                return cellId;
            }

            @Override
            public Long getEmployeeCount() {
                return employeeCount;
            }
        };
    }
}
