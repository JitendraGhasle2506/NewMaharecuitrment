package com.maharecruitment.gov.in.recruitment.service.organization;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeHierarchyNode;
import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeReportingType;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeHierarchyRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeHierarchyRepository.EmployeeRow;

@Service
@Transactional(readOnly = true)
public class EmployeeHierarchyService {
    private static final int RESPONSE_NODE_LIMIT = 500;
    private final EmployeeHierarchyRepository repository;

    public EmployeeHierarchyService(EmployeeHierarchyRepository repository) {
        this.repository = repository;
    }

    public record Option(Long id, String label) { }
    public record Options(List<Option> hods, List<Option> departments, List<Option> designations) { }
    public record SearchMatch(Long employeeId, String employeeName, String employeeCode,
                              List<EmployeeHierarchyNode> path) { }

    public Options options() {
        List<EmployeeRow> employees = repository.findActiveEmployees();
        Map<Long, String> departments = new HashMap<>();
        Map<Long, String> designations = new HashMap<>();
        List<Option> hods = new ArrayList<>();
        for (EmployeeRow employee : employees) {
            if (Boolean.TRUE.equals(employee.getHod())) {
                String code = employee.getEmployeeCode();
                hods.add(new Option(employee.getEmployeeId(), employee.getEmployeeName()
                        + (code == null || code.isBlank() ? "" : " (" + code + ")")));
            }
            if (employee.getDepartmentId() != null) departments.put(employee.getDepartmentId(), employee.getDepartment());
            if (employee.getDesignationId() != null) designations.put(employee.getDesignationId(), employee.getDesignation());
        }
        return new Options(hods, sortedOptions(departments), sortedOptions(designations));
    }

    /** A depth limit applies only to this response; any descendant can be fetched as the next branch. */
    public EmployeeHierarchyNode tree(Long rootId, Long nodeId, EmployeeReportingType type,
                                      Long departmentId, Long designationId, int offset, int limit, int depth) {
        if (offset < 0 || limit < 1 || limit > 100 || depth < 0 || depth > 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use offset >= 0, limit 1–100 and depth 0–8; expand branches for further levels.");
        }
        Graph graph = graph(rootId, type, departmentId, designationId);
        Long branchId = nodeId == null ? rootId : nodeId;
        if (!graph.included.contains(branchId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee is not in this filtered hierarchy.");
        }
        EmployeeHierarchyNode root = node(graph, branchId);
        record Pending(EmployeeHierarchyNode node, int depth, int offset) { }
        ArrayDeque<Pending> queue = new ArrayDeque<>();
        queue.add(new Pending(root, 0, offset));
        int remaining = RESPONSE_NODE_LIMIT - 1;
        while (!queue.isEmpty()) {
            Pending pending = queue.remove();
            if (pending.depth >= depth) continue;
            List<Long> children = graph.children.getOrDefault(pending.node.getEmployeeId(), List.of());
            int start = Math.min(pending.offset, children.size());
            int end = start + Math.min(Math.min(limit, remaining), children.size() - start);
            for (int index = start; index < end; index++) {
                EmployeeHierarchyNode child = node(graph, children.get(index));
                pending.node.getChildren().add(child);
                queue.add(new Pending(child, pending.depth + 1, 0));
                remaining--;
            }
            pending.node.setNextOffset(end < children.size() ? end : null);
        }
        return root;
    }

    /** Searches the complete filtered subtree, including collapsed/unloaded branches. */
    public List<SearchMatch> search(Long rootId, EmployeeReportingType type, Long departmentId,
                                     Long designationId, String query) {
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (term.length() < 2 || term.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter between 2 and 100 characters.");
        }
        Graph graph = graph(rootId, type, departmentId, designationId);
        List<SearchMatch> matches = new ArrayList<>();
        for (Long id : graph.included) {
            EmployeeRow employee = graph.employees.get(id);
            if (!graph.matches.contains(id) || !(contains(employee.getEmployeeName(), term)
                    || contains(employee.getEmployeeCode(), term))) continue;
            List<EmployeeHierarchyNode> path = new ArrayList<>();
            for (Long current = id; current != null; current = graph.parent.get(current)) {
                path.add(node(graph, current));
            }
            Collections.reverse(path);
            matches.add(new SearchMatch(id, employee.getEmployeeName(), employee.getEmployeeCode(), path));
            if (matches.size() == 20) break;
        }
        return matches;
    }

    private Graph graph(Long rootId, EmployeeReportingType type, Long departmentId, Long designationId) {
        Map<Long, EmployeeRow> employees = new LinkedHashMap<>();
        Map<Long, Long> employeeByUser = new HashMap<>();
        for (EmployeeRow employee : repository.findActiveEmployees()) {
            employees.put(employee.getEmployeeId(), employee);
            if (employee.getUserId() != null) employeeByUser.put(employee.getUserId(), employee.getEmployeeId());
        }
        if (!employees.containsKey(rootId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Selected employee is unavailable, inactive or relieved.");
        }
        Map<Long, List<Long>> edges = new HashMap<>();
        Set<Long> assigned = new HashSet<>();
        // Legacy data can contain historical duplicates. The newest mapping wins for each reporting type.
        for (var mapping : repository.findReportingRelationships(type.name())) {
            if (!assigned.add(mapping.getEmployeeId())) continue;
            Long parentId = mapping.getManagerEmployeeId() != null ? mapping.getManagerEmployeeId()
                    : employeeByUser.get(mapping.getHodUserId());
            if (parentId != null && !parentId.equals(mapping.getEmployeeId())
                    && employees.containsKey(parentId) && employees.containsKey(mapping.getEmployeeId())) {
                edges.computeIfAbsent(parentId, unused -> new ArrayList<>()).add(mapping.getEmployeeId());
            }
        }
        Comparator<Long> order = Comparator.comparing((Long id) -> employees.get(id).getEmployeeName(),
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)).thenComparing(Long::compareTo);
        edges.values().forEach(children -> children.sort(order));

        // Iterative traversal, not Java recursion: cycles are cut and hierarchy depth is unrestricted.
        Map<Long, Long> parents = new HashMap<>();
        Set<Long> visited = new LinkedHashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        visited.add(rootId);
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long parentId = queue.remove();
            for (Long child : edges.getOrDefault(parentId, List.of())) {
                if (visited.add(child)) {
                    parents.put(child, parentId);
                    queue.add(child);
                }
            }
        }
        Set<Long> matches = new HashSet<>();
        Set<Long> included = new LinkedHashSet<>();
        for (Long id : visited) {
            EmployeeRow employee = employees.get(id);
            if ((departmentId == null || Objects.equals(departmentId, employee.getDepartmentId()))
                    && (designationId == null || Objects.equals(designationId, employee.getDesignationId()))) {
                matches.add(id);
                included.add(id);
            }
        }
        // Preserve connectors through non-matching ancestors instead of incorrectly re-parenting employees.
        List<Long> reversed = new ArrayList<>(visited);
        Collections.reverse(reversed);
        for (Long id : reversed) {
            if (included.contains(id) && parents.containsKey(id)) included.add(parents.get(id));
        }
        included.add(rootId);
        Map<Long, List<Long>> children = new HashMap<>();
        for (Long id : visited) {
            if (included.contains(id) && parents.containsKey(id)) {
                children.computeIfAbsent(parents.get(id), unused -> new ArrayList<>()).add(id);
            }
        }
        // Preserve deterministic breadth-first order for search results.
        visited.retainAll(included);
        return new Graph(employees, children, parents, visited, matches);
    }

    private EmployeeHierarchyNode node(Graph graph, Long id) {
        EmployeeRow employee = graph.employees.get(id);
        int count = graph.children.getOrDefault(id, List.of()).size();
        EmployeeHierarchyNode node = new EmployeeHierarchyNode(id, employee.getEmployeeName(),
                employee.getEmployeeCode(), employee.getDesignation(), employee.getDepartment(),
                Boolean.TRUE.equals(employee.getHasPhoto()) ? "/api/employees/hierarchy/photo/" + id : null,
                count, graph.matches.contains(id));
        node.setNextOffset(count == 0 ? null : 0);
        return node;
    }

    private List<Option> sortedOptions(Map<Long, String> values) {
        return values.entrySet().stream().map(e -> new Option(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(Option::label, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term);
    }

    private record Graph(Map<Long, EmployeeRow> employees, Map<Long, List<Long>> children,
                         Map<Long, Long> parent, Set<Long> included, Set<Long> matches) { }
}
