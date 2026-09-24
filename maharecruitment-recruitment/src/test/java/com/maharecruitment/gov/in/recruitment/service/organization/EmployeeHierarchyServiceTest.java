package com.maharecruitment.gov.in.recruitment.service.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeHierarchyNode;
import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeReportingType;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeHierarchyRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeHierarchyRepository.EmployeeRow;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeHierarchyRepository.ReportingRow;

class EmployeeHierarchyServiceTest {
    private final EmployeeHierarchyRepository repository = mock(EmployeeHierarchyRepository.class);
    private final EmployeeHierarchyService service = new EmployeeHierarchyService(repository);

    @Test
    void resolvesHodUserIdAndEmployeeManagerIdIntoRecursiveTree() {
        setup(List.of(employee(1, "HOD", 10), employee(2, "Manager", 10), employee(3, "Developer", 10)),
                List.of(mapping(3, 2L), mapping(2, null)));
        EmployeeHierarchyNode root = tree(1, null, 0, 25, 3);
        assertThat(root.getEmployeeName()).isEqualTo("HOD");
        assertThat(root.getChildren()).extracting(EmployeeHierarchyNode::getEmployeeId).containsExactly(2L);
        assertThat(root.getChildren().getFirst().getChildren()).extracting(EmployeeHierarchyNode::getEmployeeId).containsExactly(3L);
        assertThat(root.getChildren().getFirst().getChildren().getFirst().isHasChildren()).isFalse();
        assertThat(root.getNextOffset()).isNull();
    }

    @Test
    void paginatesSiblingsAndAdvertisesUnloadedDescendants() {
        setup(List.of(employee(1, "HOD", 10), employee(2, "Alpha", 10), employee(3, "Beta", 10), employee(4, "Child", 10)),
                List.of(mapping(2, null), mapping(3, null), mapping(4, 2L)));
        EmployeeHierarchyNode first = tree(1, null, 0, 1, 1);
        assertThat(first.getTotalChildren()).isEqualTo(2);
        assertThat(first.getNextOffset()).isEqualTo(1);
        assertThat(first.getChildren().getFirst().getChildren()).isEmpty();
        assertThat(first.getChildren().getFirst().isHasChildren()).isTrue();
        assertThat(first.getChildren().getFirst().getNextOffset()).isZero();
        assertThat(tree(1, null, 1, 1, 1).getChildren()).extracting(EmployeeHierarchyNode::getEmployeeId).containsExactly(3L);
        assertThat(tree(1, 2L, 0, 25, 1).getChildren()).extracting(EmployeeHierarchyNode::getEmployeeId).containsExactly(4L);
        assertThat(tree(1, null, Integer.MAX_VALUE, 25, 1).getChildren()).isEmpty();
    }

    @Test
    void cutsCircularAndSelfReportingRelationships() {
        setup(List.of(employee(1, "HOD", 10), employee(2, "Manager", 10), employee(3, "Leaf", 10), employee(4, "Self", 10)),
                List.of(mapping(1, 3L), mapping(2, 1L), mapping(3, 2L), mapping(4, 4L)));
        EmployeeHierarchyNode root = tree(1, null, 0, 25, 8);
        assertThat(root.getChildren().getFirst().getChildren().getFirst().getChildren()).isEmpty();
        assertThat(service.search(1L, EmployeeReportingType.PRIMARY, null, null, "leaf").getFirst().path())
                .extracting(EmployeeHierarchyNode::getEmployeeId).containsExactly(1L, 2L, 3L);
    }

    @Test
    void newestAssignmentWinsWithoutFallingBackToAnOlderOrInactiveManager() {
        setup(List.of(employee(1, "HOD", 10), employee(2, "Manager", 10), employee(3, "Leaf", 10)),
                List.of(mapping(3, 999L), mapping(3, 1L), mapping(2, null)));
        assertThat(tree(1, null, 0, 25, 8).getChildren()).extracting(EmployeeHierarchyNode::getEmployeeId).containsExactly(2L);
        assertThatThrownBy(() -> tree(1, 999L, 0, 25, 1)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> tree(999, null, 0, 25, 1)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void filtersKeepAncestorPathAndSearchesUnloadedEmployeesByCode() {
        setup(List.of(employee(1, "HOD", 10), employee(2, "Manager", 10), employee(3, "Developer", 20), employee(4, "Other", 10)),
                List.of(mapping(2, null), mapping(3, 2L), mapping(4, null)));
        EmployeeHierarchyNode filtered = service.tree(1L, null, EmployeeReportingType.PRIMARY, 20L, 30L, 0, 25, 1);
        assertThat(filtered.isFilterMatch()).isFalse();
        assertThat(filtered.getChildren()).extracting(EmployeeHierarchyNode::getEmployeeId).containsExactly(2L);
        var result = service.search(1L, EmployeeReportingType.PRIMARY, 20L, 30L, "EMP3");
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().path()).extracting(EmployeeHierarchyNode::getEmployeeId).containsExactly(1L, 2L, 3L);
        assertThat(service.search(1L, EmployeeReportingType.PRIMARY, 20L, null, "Manager")).isEmpty();
        assertThat(service.tree(1L, null, EmployeeReportingType.PRIMARY, 99L, null, 0, 25, 1).isHasChildren()).isFalse();
    }

    @Test
    void reportingTypesAreIsolatedAndInputsAreBounded() {
        setup(List.of(employee(1, "HOD", 10), employee(2, "Manager", 10)), List.of(mapping(2, null)));
        when(repository.findReportingRelationships("PROJECT")).thenReturn(List.of());
        assertThat(service.tree(1L, null, EmployeeReportingType.PROJECT, null, null, 0, 25, 1).getChildren()).isEmpty();
        verify(repository).findReportingRelationships("PROJECT");
        assertThatThrownBy(() -> tree(1, null, -1, 25, 1)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> tree(1, null, 0, 101, 1)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> tree(1, null, 0, 25, 9)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.search(1L, EmployeeReportingType.PRIMARY, null, null, "a"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void supportsVeryDeepTreesWithoutRecursiveStackGrowth() {
        List<EmployeeRow> employees = new ArrayList<>();
        List<ReportingRow> mappings = new ArrayList<>();
        for (int id = 1; id <= 2000; id++) {
            employees.add(employee(id, "Person " + id, 10));
            if (id > 1) mappings.add(mapping(id, (long) id - 1));
        }
        setup(employees, mappings);
        assertThat(tree(1, 1999L, 0, 25, 1).getChildren().getFirst().getEmployeeId()).isEqualTo(2000L);
        assertThat(service.search(1L, EmployeeReportingType.PRIMARY, null, null, "EMP2000").getFirst().path()).hasSize(2000);
    }

    @Test
    void capsBroadRecursiveResponsesAndLeavesContinuationCursors() {
        List<EmployeeRow> employees = new ArrayList<>();
        List<ReportingRow> mappings = new ArrayList<>();
        employees.add(employee(1, "HOD", 10));
        for (int id = 2; id <= 1101; id++) {
            employees.add(employee(id, "Person " + id, 10));
            mappings.add(mapping(id, id <= 101 ? 1L : 2L + (id - 102) / 10));
        }
        setup(employees, mappings);
        EmployeeHierarchyNode root = tree(1, null, 0, 100, 8);
        int count = 1 + root.getChildren().size() + root.getChildren().stream().mapToInt(n -> n.getChildren().size()).sum();
        assertThat(count).isEqualTo(500);
        assertThat(root.getChildren()).anyMatch(node -> node.getNextOffset() != null);
    }

    private EmployeeHierarchyNode tree(long root, Long node, int offset, int limit, int depth) {
        return service.tree(root, node, EmployeeReportingType.PRIMARY, null, null, offset, limit, depth);
    }

    private void setup(List<EmployeeRow> employees, List<ReportingRow> mappings) {
        when(repository.findActiveEmployees()).thenReturn(employees);
        when(repository.findReportingRelationships("PRIMARY")).thenReturn(mappings);
    }

    private EmployeeRow employee(long id, String name, long department) {
        EmployeeRow employee = mock(EmployeeRow.class);
        when(employee.getEmployeeId()).thenReturn(id);
        when(employee.getUserId()).thenReturn(id + 100);
        when(employee.getEmployeeName()).thenReturn(name);
        when(employee.getEmployeeCode()).thenReturn("EMP" + id);
        when(employee.getDepartmentId()).thenReturn(department);
        when(employee.getDesignationId()).thenReturn(30L);
        when(employee.getHod()).thenReturn(id == 1);
        return employee;
    }

    private ReportingRow mapping(long child, Long parent) {
        ReportingRow mapping = mock(ReportingRow.class);
        when(mapping.getEmployeeId()).thenReturn(child);
        when(mapping.getManagerEmployeeId()).thenReturn(parent);
        when(mapping.getHodUserId()).thenReturn(101L);
        return mapping;
    }
}
