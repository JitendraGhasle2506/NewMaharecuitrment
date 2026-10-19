package com.maharecruitment.gov.in.recruitment.service.organization;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.maharecruitment.gov.in.recruitment.repository.EmployeeHierarchyRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeHierarchyRepository.EmployeeRow;

import lombok.RequiredArgsConstructor;

/** Resolves permanent reporting ownership using two bulk projections and linear graph traversal. */
@Component
@RequiredArgsConstructor
public class ResourceReportingScopeResolver {
    private final EmployeeHierarchyRepository hierarchy;

    public record Scope(Long self, Map<Long, EmployeeRow> people, Map<Long, Long> parents, Set<Long> team) { }

    public Scope resolve(Long userId) {
        Map<Long, EmployeeRow> people = new LinkedHashMap<>();
        Map<Long, Long> byUser = new HashMap<>();
        for (EmployeeRow employee : hierarchy.findActiveEmployees()) {
            people.put(employee.getEmployeeId(), employee);
            if (employee.getUserId() != null) byUser.put(employee.getUserId(), employee.getEmployeeId());
        }
        Long self = byUser.get(userId);
        if (self == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Your account must be linked to an active employee to manage resources.");
        Map<Long, Long> parents = new HashMap<>();
        Map<Long, List<Long>> children = new HashMap<>();
        Set<Long> seen = new HashSet<>();
        for (var row : hierarchy.findReportingRelationships("PRIMARY")) {
            // Results are newest first. Never fall back to an older assignment, even if the latest is invalid.
            if (!seen.add(row.getEmployeeId()) || !people.containsKey(row.getEmployeeId())) continue;
            Long parent = row.getManagerEmployeeId() == null ? byUser.get(row.getHodUserId()) : row.getManagerEmployeeId();
            if (parent == null || parent.equals(row.getEmployeeId()) || !people.containsKey(parent)) continue;
            parents.put(row.getEmployeeId(), parent);
            children.computeIfAbsent(parent, ignored -> new ArrayList<>()).add(row.getEmployeeId());
        }
        Set<Long> ancestors = new HashSet<>();
        for (Long id = self; id != null; id = parents.get(id)) {
            if (!ancestors.add(id)) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Your reporting chain contains a circular assignment. Please ask HR to correct it.");
        }
        Set<Long> team = new HashSet<>();
        ArrayDeque<Long> pending = new ArrayDeque<>();
        pending.add(self);
        while (!pending.isEmpty()) {
            Long id = pending.remove();
            if (team.add(id)) pending.addAll(children.getOrDefault(id, List.of()));
        }
        return new Scope(self, people, parents, team);
    }
}
