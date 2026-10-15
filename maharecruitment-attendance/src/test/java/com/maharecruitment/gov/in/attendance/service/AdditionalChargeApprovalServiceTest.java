package com.maharecruitment.gov.in.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.maharecruitment.gov.in.recruitment.repository.AdditionalChargeRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeHierarchyRepository;
import com.maharecruitment.gov.in.recruitment.service.ReportingManagerService;

class AdditionalChargeApprovalServiceTest {
    final AdditionalChargeRepository charges = mock(AdditionalChargeRepository.class);
    final EmployeeHierarchyRepository hierarchy = mock(EmployeeHierarchyRepository.class);
    final ReportingManagerService reporting = mock(ReportingManagerService.class);
    final AdditionalChargeApprovalService service = new AdditionalChargeApprovalService(charges, hierarchy, reporting);

    @BeforeEach void setup() {
        doReturn(List.of(charge(2L))).when(charges).activeApprovalCharges(LocalDate.now());
        doReturn(List.of(employee(1, 11), employee(2, 22), employee(3, 33), employee(4, 44), employee(5, 55)))
                .when(hierarchy).findActiveEmployees();
        doReturn(List.of(mapping(3, 2), mapping(4, 2))).when(hierarchy).findReportingRelationships("PRIMARY");
        doReturn(List.of(cellMember(1, 8), cellMember(2, 8), cellMember(3, 8), cellMember(4, 9), cellMember(5, 8)))
                .when(charges).approvalCellMembers(java.util.Set.of(8L));
    }

    @Test void inheritedApprovalsStayWithinCellSubtreeAndOriginalStage() {
        var scope = service.snapshot();
        assertThat(scope.canAct(11L, 2L, "MANAGER", 7L, 9L)).isTrue();
        assertThat(scope.canAct(11L, 3L, "HOD", 9L, 7L)).isTrue();
        assertThat(scope.canAct(7L, 2L, "MANAGER", 7L, 9L)).isFalse();
        assertThat(scope.canAct(11L, 2L, "HOD", 7L, 9L)).isFalse();
        assertThat(scope.canAct(9L, 2L, "HOD", 7L, 9L)).isTrue();
        assertThat(scope.canAct(11L, 4L, "HOD", 9L, 7L)).isFalse();
        assertThat(scope.canAct(11L, 5L, "HOD", 9L, 7L)).isFalse();
        assertThat(scope.incomingEmployeeIds(11L)).containsExactlyInAnyOrder(2L, 3L);
        assertThat(scope.canAct(null, 2L, "HOD", 9L, 7L)).isFalse();
    }

    @Test void endingOrRevokingCoverageRestoresOriginalApproverForPendingRequests() {
        assertThat(service.snapshot().effectiveApprover(2L, 7L)).isEqualTo(11L);
        when(charges.activeApprovalCharges(LocalDate.now())).thenReturn(List.of());
        var scope = service.snapshot();
        assertThat(scope.canAct(7L, 2L, "HOD", 9L, 7L)).isTrue();
        assertThat(scope.canAct(11L, 2L, "HOD", 9L, 7L)).isFalse();
        assertThat(scope.affects(11L)).isFalse();
    }

    @Test void allCellChargeCoversTheInheritedSubtreeAcrossCellsAndUnassignedEmployees() {
        var global = charge(2L);
        when(global.getCellId()).thenReturn(null);
        when(charges.activeApprovalCharges(LocalDate.now())).thenReturn(List.of(global));
        var scope = service.snapshot();
        assertThat(scope.incomingEmployeeIds(11L)).containsExactlyInAnyOrder(2L, 3L, 4L);
        assertThat(scope.canAct(11L, 4L, "HOD", 9L, 7L)).isTrue();
        assertThat(scope.canAct(11L, 5L, "HOD", 9L, 7L)).isFalse();
        verify(charges, never()).approvalCellMembers(any());
    }

    @Test void additionalChargeNeverAllowsSelfApproval() {
        doReturn(List.of(mapping(1, 2), mapping(3, 2))).when(hierarchy).findReportingRelationships("PRIMARY");
        var scope = service.snapshot();
        assertThat(scope.canAct(11L, 1L, "MANAGER", 7L, 9L)).isFalse();
        assertThat(scope.incomingEmployeeIds(11L)).doesNotContain(1L);
    }

    @Test void legacyDelegationCannotExceedIncumbentsActualAuthorityAndCachesSourceLookup() {
        when(reporting.getEffectiveEmployeeIdsForAuthority(7L)).thenReturn(List.of(2L));
        var scope = service.snapshot();
        assertThat(scope.canActLegacy(11L, 2L, List.of())).isTrue();
        assertThat(scope.canActLegacy(11L, 3L, List.of())).isFalse();
        assertThat(scope.canActLegacy(7L, 2L, List.of(2L))).isFalse();
        assertThat(scope.canActLegacy(11L, 5L, List.of(5L))).isTrue();
        verify(reporting, times(1)).getEffectiveEmployeeIdsForAuthority(7L);
    }

    @Test void movedRootAndInactiveChargeHolderDoNotGrantAuthority() {
        doReturn(List.of(cellMember(3, 8))).when(charges).approvalCellMembers(java.util.Set.of(8L));
        assertThat(service.snapshot().incomingEmployeeIds(11L)).isEmpty();
        doReturn(List.of(employee(2, 22), employee(3, 33))).when(hierarchy).findActiveEmployees();
        assertThat(service.snapshot().affects(11L)).isFalse();
    }

    @Test void latestPrimaryMappingWinsAndCyclesTerminate() {
        doReturn(List.of(mapping(3, 5), mapping(3, 2), mapping(2, 2))).when(hierarchy).findReportingRelationships("PRIMARY");
        var scope = service.snapshot();
        assertThat(scope.incomingEmployeeIds(11L)).containsExactly(2L);
    }

    private AdditionalChargeRepository.ApprovalCharge charge(Long memberId) {
        var row = mock(AdditionalChargeRepository.ApprovalCharge.class);
        when(row.getChargeId()).thenReturn(100L);
        when(row.getEmployeeId()).thenReturn(memberId);
        when(row.getHolderEmployeeId()).thenReturn(1L);
        when(row.getHolderUserId()).thenReturn(11L);
        when(row.getSourceUserId()).thenReturn(7L);
        when(row.getCellId()).thenReturn(8L);
        return row;
    }
    private EmployeeHierarchyRepository.EmployeeRow employee(long id, long user) {
        var row = mock(EmployeeHierarchyRepository.EmployeeRow.class);
        when(row.getEmployeeId()).thenReturn(id);
        when(row.getUserId()).thenReturn(user);
        return row;
    }
    private EmployeeHierarchyRepository.ReportingRow mapping(long child, long parent) {
        var row = mock(EmployeeHierarchyRepository.ReportingRow.class);
        when(row.getEmployeeId()).thenReturn(child);
        when(row.getManagerEmployeeId()).thenReturn(parent);
        return row;
    }
    private AdditionalChargeRepository.CellMember cellMember(long employee, long cell) {
        var row = mock(AdditionalChargeRepository.CellMember.class);
        when(row.getEmployeeId()).thenReturn(employee);
        when(row.getCellId()).thenReturn(cell);
        return row;
    }
}
