package com.maharecruitment.gov.in.recruitment.service.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeHierarchyRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeHierarchyRepository.EmployeeRow;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeHierarchyRepository.ReportingRow;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.service.organization.ResourceManagementService.EmployeeTypeFilter;

class ResourceManagementServiceTest {
    private final EmployeeHierarchyRepository hierarchy = mock(EmployeeHierarchyRepository.class);
    private final EmployeeReportingMappingRepository mappings = mock(EmployeeReportingMappingRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final com.maharecruitment.gov.in.audit.service.AuditTrailService auditTrail = mock(
            com.maharecruitment.gov.in.audit.service.AuditTrailService.class);
    private final ResourceManagementService service = new ResourceManagementService(
            new ResourceReportingScopeResolver(hierarchy), mappings, employees, auditTrail);

    @BeforeEach
    void setUp() {
        doReturn(List.of(
                person(1, 10L, "Manager"), person(2, 20L, "Team lead"), person(3, null, "Engineer"),
                person(4, null, "Available engineer"), person(5, 50L, "Other manager"))).when(hierarchy).findActiveEmployees();
        doReturn(List.of(link(3, 2L, null), link(2, 1L, null))).when(hierarchy).findReportingRelationships("PRIMARY");
        when(mappings.findMappedResourceIds()).thenReturn(List.of(2L, 3L, 5L));
        when(employees.lockForResourceMapping(any())).thenReturn(Optional.of(new EmployeeEntity()));
    }

    @Test
    void listsOnlyOwnTeamWithDirectAndIndirectReportsAndClampsPagination() {
        var page = service.list(10L, "mapped", "", 99, 1);
        assertThat(page.total()).isEqualTo(2);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.mappedCount()).isEqualTo(2);
        assertThat(page.directCount()).isEqualTo(1);
        assertThat(page.resources()).extracting(ResourceManagementService.Resource::employeeId).containsExactly(3L);
        assertThat(page.resources().getFirst().manager()).isEqualTo("Team lead");
        assertThat(page.managers()).extracting(ResourceManagementService.Manager::employeeId).containsExactly(1L, 2L, 3L);
    }

    @Test
    void freeListExcludesSelfAndEveryExistingMappingTypeAndSupportsSearch() {
        var page = service.list(10L, "free", "AVAILABLE", 0, 12);
        assertThat(page.resources()).extracting(ResourceManagementService.Resource::employeeId).containsExactly(4L);
        assertThat(page.freeCount()).isEqualTo(1);
        assertThat(service.list(10L, "free", "missing", 0, 12).total()).isZero();
    }

    @Test
    void defaultsFreeResourcesToInternalAndMahaitAndAllowsEachEmployeeType() {
        var internal = person(4L, null, "Internal resource");
        var mahait = person(6L, null, "MAHAIT resource");
        var external = person(7L, null, "External resource");
        var unknown = person(8L, null, "Unknown resource");
        when(mahait.getRecruitmentType()).thenReturn(" mahait ");
        when(external.getRecruitmentType()).thenReturn("EXTERNAL");
        when(unknown.getRecruitmentType()).thenReturn(null);
        doReturn(List.of(person(1L, 10L, "Manager"), internal, mahait, external, unknown))
                .when(hierarchy).findActiveEmployees();

        var defaultPage = service.list(10L, "free", "", 0, 12);
        assertThat(defaultPage.resources()).extracting(ResourceManagementService.Resource::employeeId).containsExactly(4L, 6L);
        assertThat(defaultPage.freeCount()).isEqualTo(2);
        assertThat(service.list(10L, "free", "", 0, 12, EmployeeTypeFilter.INTERNAL).resources())
                .extracting(ResourceManagementService.Resource::employeeId).containsExactly(4L);
        assertThat(service.list(10L, "free", "", 0, 12, EmployeeTypeFilter.MAHAIT).resources())
                .extracting(ResourceManagementService.Resource::employeeId).containsExactly(6L);
        var externalPage = service.list(10L, "free", "external", 99, 1, EmployeeTypeFilter.EXTERNAL);
        assertThat(externalPage.resources()).extracting(ResourceManagementService.Resource::employeeId).containsExactly(7L);
        assertThat(externalPage.page()).isZero();
        assertThat(externalPage.freeCount()).isEqualTo(1);
        assertThat(service.list(10L, "free", "", 0, 12, EmployeeTypeFilter.ALL).total()).isEqualTo(4);
        assertThat(service.list(10L, "free", "External", 0, 12).total()).isZero();
    }

    @Test
    void employeeTypeFilterDoesNotRestrictMappedTeamOrReportingManagerOptions() {
        var page = service.list(10L, "mapped", "", 0, 12, EmployeeTypeFilter.EXTERNAL);
        assertThat(page.resources()).extracting(ResourceManagementService.Resource::employeeId).containsExactly(2L, 3L);
        assertThat(page.managers()).extracting(ResourceManagementService.Manager::employeeId).containsExactly(1L, 2L, 3L);
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 2L, 3L})
    void mapsFreeResourceDirectlyToSelfOrAnyLevelOfSubordinate(long managerId) {
        service.map(10L, 4L, managerId);
        var mapping = org.mockito.ArgumentCaptor.forClass(EmployeeReportingMappingEntity.class);
        verify(mappings).save(mapping.capture());
        assertThat(mapping.getValue().getEmployeeId()).isEqualTo(4L);
        assertThat(mapping.getValue().getManagerEmployeeId()).isEqualTo(managerId);
        assertThat(mapping.getValue().getHodUserId()).isEqualTo(10L);
        assertThat(mapping.getValue().getReportingType()).isEqualTo("PRIMARY");
        var order = inOrder(employees, hierarchy, mappings);
        order.verify(employees).lockForResourceMapping(4L);
        order.verify(hierarchy).findActiveEmployees();
        order.verify(hierarchy).findReportingRelationships("PRIMARY");
        order.verify(mappings).findByEmployeeIdIn(List.of(4L));
        order.verify(mappings).save(any());
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 2L})
    void changesReportingManagerForExistingTeamResource(long managerId) {
        var latest = mapping(20L, "PRIMARY");
        latest.setProjectId(101L);
        var older = mapping(10L, "PRIMARY");
        var project = mapping(30L, "PROJECT");
        when(mappings.findByEmployeeIdIn(List.of(3L))).thenReturn(List.of(older, latest, project));
        service.reassign(10L, 3L, managerId);
        verify(mappings).save(latest);
        assertThat(latest.getManagerEmployeeId()).isEqualTo(managerId);
        assertThat(latest.getProjectId()).isEqualTo(101L);
        assertThat(project.getManagerEmployeeId()).isNull();
        assertThat(older.getManagerEmployeeId()).isNull();
        verify(mappings, never()).deletePrimaryResourceMappings(any());
    }

    @Test
    void normalizesLegacyPrimaryTypeWhenChangingReportingManager() {
        var legacy = mapping(20L, null);
        when(mappings.findByEmployeeIdIn(List.of(3L))).thenReturn(List.of(legacy));
        service.reassign(10L, 3L, 1L);
        assertThat(legacy.getReportingType()).isEqualTo("PRIMARY");
        verify(mappings).save(legacy);
    }

    @Test
    void rejectsReassigningAnotherTeamsResourceOrSelectingAnOutsideManager() {
        assertStatus(() -> service.reassign(10L, 5L, 2L), HttpStatus.FORBIDDEN);
        assertStatus(() -> service.reassign(10L, 3L, 5L), HttpStatus.FORBIDDEN);
        assertStatus(() -> service.reassign(10L, 1L, 2L), HttpStatus.FORBIDDEN);
        verify(mappings, never()).save(any());
    }

    @Test
    void rejectsMovingAManagerUnderItsOwnDescendantOrItself() {
        assertStatus(() -> service.reassign(10L, 2L, 3L), HttpStatus.CONFLICT);
        assertStatus(() -> service.reassign(10L, 2L, 2L), HttpStatus.CONFLICT);
        verify(mappings, never()).save(any());
    }

    @Test
    void rejectsReassignmentWhenPrimaryRecordIsNoLongerAvailable() {
        assertStatus(() -> service.reassign(10L, 3L, 1L), HttpStatus.CONFLICT);
        verify(mappings, never()).save(any());
    }

    @Test
    void rejectsAlreadyMappedResourceAfterObtainingTheLock() {
        when(mappings.findByEmployeeIdIn(List.of(4L))).thenReturn(List.of(new EmployeeReportingMappingEntity()));
        assertStatus(() -> service.map(10L, 4L, 1L), HttpStatus.CONFLICT);
        verify(mappings, never()).save(any());
    }

    @Test
    void rejectsManagerOutsideOwnTeamAndUnavailableResources() {
        assertStatus(() -> service.map(10L, 4L, 5L), HttpStatus.FORBIDDEN);
        assertStatus(() -> service.map(10L, 4L, null), HttpStatus.FORBIDDEN);
        assertStatus(() -> service.map(10L, 999L, 1L), HttpStatus.CONFLICT);
        verify(mappings, never()).save(any());
    }

    @Test
    void preventsSelfAssignmentAndAssigningAnAncestorUnderItsDescendant() {
        assertStatus(() -> service.map(10L, 1L, 1L), HttpStatus.CONFLICT);
        // This user's otherwise-unmapped manager is not safe to assign under a descendant.
        doReturn(List.of(link(1, 5L, null), link(2, 1L, null))).when(hierarchy).findReportingRelationships("PRIMARY");
        assertStatus(() -> service.map(10L, 5L, 2L), HttpStatus.CONFLICT);
        verify(mappings, never()).save(any());
    }

    @Test
    void releasesIndirectResourceAndDeletesAllPrimaryHistoryOnly() {
        service.release(10L, 3L);
        verify(mappings).deletePrimaryResourceMappings(3L);
        verify(mappings, never()).deleteByEmployeeId(any());
    }

    @Test
    void refusesToReleaseSelfOrAnotherManagersResource() {
        assertStatus(() -> service.release(10L, 1L), HttpStatus.FORBIDDEN);
        assertStatus(() -> service.release(10L, 5L), HttpStatus.FORBIDDEN);
        verify(mappings, never()).deletePrimaryResourceMappings(any());
    }

    @Test
    void missingOrInactiveSessionEmployeeHasNoAccess() {
        assertStatus(() -> service.list(99L, "mapped", "", 0, 12), HttpStatus.FORBIDDEN);
    }

    @Test
    void usesLatestMappingAndLegacyHodFallbackWithoutRestoringOldAssignments() {
        doReturn(List.of(link(3, 999L, null), link(3, 1L, null), link(2, null, 10L)))
                .when(hierarchy).findReportingRelationships("PRIMARY");
        assertThat(service.list(10L, "mapped", "", 0, 12).resources())
                .extracting(ResourceManagementService.Resource::employeeId).containsExactly(2L);
    }

    @Test
    void corruptedCircularChainCannotGrantWriteAccess() {
        doReturn(List.of(link(1, 2L, null), link(2, 1L, null))).when(hierarchy).findReportingRelationships("PRIMARY");
        assertStatus(() -> service.release(10L, 2L), HttpStatus.CONFLICT);
        verify(mappings, never()).deletePrimaryResourceMappings(any());
    }

    @Test
    void validatesQueryBoundsBeforeReadingData() {
        assertStatus(() -> service.list(10L, "all", "", 0, 12), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.list(10L, "mapped", "", -1, 12), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.list(10L, "mapped", "", 0, 101), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.list(10L, "mapped", "x".repeat(101), 0, 12), HttpStatus.BAD_REQUEST);
        verify(hierarchy, never()).findActiveEmployees();
    }

    private static void assertStatus(Runnable action, HttpStatus status) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class,
                error -> assertThat(error.getStatusCode()).isEqualTo(status));
    }

    private static EmployeeReportingMappingEntity mapping(Long id, String type) {
        var mapping = new EmployeeReportingMappingEntity();
        mapping.setMappingId(id);
        mapping.setEmployeeId(3L);
        mapping.setReportingType(type);
        return mapping;
    }

    private static EmployeeRow person(long id, Long user, String name) {
        EmployeeRow row = mock(EmployeeRow.class);
        when(row.getEmployeeId()).thenReturn(id);
        when(row.getUserId()).thenReturn(user);
        when(row.getEmployeeName()).thenReturn(name);
        when(row.getEmployeeCode()).thenReturn("EMP" + id);
        when(row.getRecruitmentType()).thenReturn("INTERNAL");
        return row;
    }

    private static ReportingRow link(long employeeId, Long manager, Long hod) {
        ReportingRow row = mock(ReportingRow.class);
        when(row.getEmployeeId()).thenReturn(employeeId);
        when(row.getManagerEmployeeId()).thenReturn(manager);
        when(row.getHodUserId()).thenReturn(hod);
        return row;
    }
}
