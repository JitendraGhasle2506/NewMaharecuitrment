package com.maharecruitment.gov.in.recruitment.service.organization;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeHierarchyRepository.EmployeeRow;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.service.organization.ResourceReportingScopeResolver.Scope;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Manages permanent PRIMARY assignments; temporary approval coverage grants no write access. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ResourceManagementService {
    private final ResourceReportingScopeResolver scopes;
    private final EmployeeReportingMappingRepository mappings;
    private final EmployeeRepository employees;

    public record Resource(Long employeeId, String name, String code, String designation,
                           String department, String manager, boolean direct) { }
    public record Manager(Long employeeId, String name) { }
    public record Page(List<Resource> resources, List<Manager> managers, int total, int page,
                       int size, int mappedCount, int directCount, int freeCount) { }

    public Page list(Long userId, String view, String query, int page, int size) {
        if (!Set.of("mapped", "free").contains(view) || page < 0 || size < 1 || size > 100
                || (query != null && query.length() > 100)) {
            throw error(HttpStatus.BAD_REQUEST, "Use mapped/free, page >= 0, size 1–100 and search up to 100 characters.");
        }
        Scope s = scopes.resolve(userId);
        Set<Long> assigned = new HashSet<>(mappings.findMappedResourceIds());
        String term = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        List<Resource> found = new ArrayList<>();
        List<Manager> managers = new ArrayList<>();
        managers.add(new Manager(s.self(), "Me · " + s.people().get(s.self()).getEmployeeName()));
        int direct = 0, free = 0;
        for (EmployeeRow e : s.people().values()) {
            Long id = e.getEmployeeId();
            boolean inTeam = !id.equals(s.self()) && s.team().contains(id);
            boolean isDirect = s.self().equals(s.parents().get(id));
            boolean available = !id.equals(s.self()) && !assigned.contains(id);
            if (inTeam) managers.add(new Manager(id, e.getEmployeeName()
                    + (e.getEmployeeCode() == null ? "" : " (" + e.getEmployeeCode() + ")")));
            if (inTeam && isDirect) direct++;
            if (available) free++;
            if (!(view.equals("mapped") ? inTeam : available)) continue;
            EmployeeRow parent = s.people().get(s.parents().get(id));
            Resource row = new Resource(id, e.getEmployeeName(), e.getEmployeeCode(), e.getDesignation(),
                    e.getDepartment(), parent == null ? null : parent.getEmployeeName(), isDirect);
            if (term.isEmpty() || String.join(" ", text(row.name), text(row.code), text(row.designation),
                    text(row.department), text(row.manager)).toLowerCase(Locale.ROOT).contains(term)) found.add(row);
        }
        int total = found.size();
        int actualPage = Math.min(page, Math.max(0, (total - 1) / size));
        int start = actualPage * size;
        return new Page(List.copyOf(found.subList(start, Math.min(start + size, total))), managers,
                total, actualPage, size, s.team().size() - 1, direct, free);
    }

    @Transactional
    public void map(Long userId, Long employeeId, Long managerId) {
        lock(employeeId);
        Scope s = scopes.resolve(userId);
        requireActive(s, employeeId);
        if (managerId == null || !s.team().contains(managerId)) {
            throw error(HttpStatus.FORBIDDEN, "Choose yourself or a subordinate as the reporting person.");
        }
        if (!mappings.findByEmployeeIdIn(List.of(employeeId)).isEmpty()) {
            throw error(HttpStatus.CONFLICT, "This resource has already been mapped. Refresh the list.");
        }
        Set<Long> visited = new HashSet<>();
        for (Long id = managerId; id != null && visited.add(id); id = s.parents().get(id)) {
            if (id.equals(employeeId)) throw error(HttpStatus.CONFLICT, "This assignment would create a reporting cycle.");
        }
        EmployeeReportingMappingEntity mapping = new EmployeeReportingMappingEntity();
        mapping.setEmployeeId(employeeId);
        mapping.setManagerEmployeeId(managerId);
        // Keep the authority reference compatible with existing approval/reporting consumers.
        mapping.setHodUserId(userId);
        mapping.setManagerType("OTHER");
        mapping.setReportingType("PRIMARY");
        mappings.save(mapping);
        log.info("Resource mapped: actorUserId={}, employeeId={}, managerEmployeeId={}", userId, employeeId, managerId);
    }

    @Transactional
    public void release(Long userId, Long employeeId) {
        lock(employeeId);
        Scope s = scopes.resolve(userId);
        if (employeeId.equals(s.self()) || !s.team().contains(employeeId)) {
            throw error(HttpStatus.FORBIDDEN, "You can release only resources assigned to you or your subordinates.");
        }
        // Remove older PRIMARY rows too, so a superseded assignment cannot become active again.
        int removed = mappings.deletePrimaryResourceMappings(employeeId);
        log.info("Resource released: actorUserId={}, employeeId={}, primaryMappingsRemoved={}", userId, employeeId, removed);
    }

    private void lock(Long id) {
        if (id == null) throw error(HttpStatus.BAD_REQUEST, "Select a resource.");
        employees.lockForResourceMapping(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Resource no longer exists."));
    }

    private void requireActive(Scope s, Long id) {
        if (!s.people().containsKey(id)) throw error(HttpStatus.CONFLICT, "Resource is inactive or unavailable.");
    }

    private static String text(String value) { return value == null ? "" : value; }
    private static ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
