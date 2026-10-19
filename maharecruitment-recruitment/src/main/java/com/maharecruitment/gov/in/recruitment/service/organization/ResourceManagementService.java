package com.maharecruitment.gov.in.recruitment.service.organization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;
import com.maharecruitment.gov.in.audit.dto.AuditRecordRequest;
import com.maharecruitment.gov.in.audit.service.AuditTrailService;
import com.maharecruitment.gov.in.common.audit.AuditRequestContext;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeRecruitmentType;
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
    private final AuditTrailService auditTrail;

    public record Resource(Long employeeId, String name, String code, String designation,
                           String department, String manager, Long managerEmployeeId, boolean direct, String employeeType) { }
    public record Manager(Long employeeId, String name) { }
    public record Page(List<Resource> resources, List<Manager> managers, int total, int page,
                       int size, int mappedCount, int directCount, int freeCount) { }

    public enum EmployeeTypeFilter {
        INTERNAL_MAHAIT, INTERNAL, EXTERNAL, MAHAIT, ALL;

        boolean matches(String recruitmentType) {
            String type = EmployeeRecruitmentType.normalizeOrNull(recruitmentType);
            return switch (this) {
                case ALL -> true;
                case INTERNAL_MAHAIT -> "INTERNAL".equals(type) || "MAHAIT".equals(type);
                default -> name().equals(type);
            };
        }
    }

    public Page list(Long userId, String view, String query, int page, int size) {
        return list(userId, view, query, page, size, EmployeeTypeFilter.INTERNAL_MAHAIT);
    }

    public Page list(Long userId, String view, String query, int page, int size, EmployeeTypeFilter employeeType) {
        if (!Set.of("mapped", "free").contains(view) || page < 0 || size < 1 || size > 100
                || employeeType == null || (query != null && query.length() > 100)) {
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
            boolean available = !id.equals(s.self()) && !assigned.contains(id) && employeeType.matches(e.getRecruitmentType());
            if (inTeam) managers.add(new Manager(id, e.getEmployeeName()
                    + (e.getEmployeeCode() == null ? "" : " (" + e.getEmployeeCode() + ")")));
            if (inTeam && isDirect) direct++;
            if (available) free++;
            if (!(view.equals("mapped") ? inTeam : available)) continue;
            EmployeeRow parent = s.people().get(s.parents().get(id));
            Resource row = new Resource(id, e.getEmployeeName(), e.getEmployeeCode(), e.getDesignation(),
                    e.getDepartment(), parent == null ? null : parent.getEmployeeName(), s.parents().get(id), isDirect,
                    EmployeeRecruitmentType.normalizeOrNull(e.getRecruitmentType()));
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
        validateManager(s, employeeId, managerId);
        if (!mappings.findByEmployeeIdIn(List.of(employeeId)).isEmpty()) {
            throw error(HttpStatus.CONFLICT, "This resource has already been mapped. Refresh the list.");
        }
        EmployeeReportingMappingEntity mapping = new EmployeeReportingMappingEntity();
        mapping.setEmployeeId(employeeId);
        mapping.setManagerEmployeeId(managerId);
        // Keep the authority reference compatible with existing approval/reporting consumers.
        mapping.setHodUserId(userId);
        mapping.setManagerType("OTHER");
        mapping.setReportingType("PRIMARY");
        mappings.save(mapping);
        recordAction(userId, s, employeeId, "RESOURCE_MAPPED", List.of(), List.of(mappingSnapshot(mapping)));
        log.info("Resource mapped: actorUserId={}, employeeId={}, managerEmployeeId={}", userId, employeeId, managerId);
    }

    @Transactional
    public void reassign(Long userId, Long employeeId, Long managerId) {
        lock(employeeId);
        Scope scope = scopes.resolve(userId);
        requireTeamResource(scope, employeeId);
        validateManager(scope, employeeId, managerId);
        EmployeeReportingMappingEntity mapping = mappings.findByEmployeeIdIn(List.of(employeeId)).stream()
                .filter(row -> row.getReportingType() == null || "PRIMARY".equals(row.getReportingType()))
                .max(Comparator.comparing(EmployeeReportingMappingEntity::getMappingId))
                .orElseThrow(() -> error(HttpStatus.CONFLICT, "The reporting assignment has changed. Refresh the list."));
        List<Map<String, Object>> previous = List.of(mappingSnapshot(mapping));
        mapping.setManagerEmployeeId(managerId);
        mapping.setHodUserId(userId);
        mapping.setManagerType("OTHER");
        mapping.setReportingType("PRIMARY");
        mappings.save(mapping);
        recordAction(userId, scope, employeeId, "RESOURCE_REASSIGNED", previous, List.of(mappingSnapshot(mapping)));
        log.info("Resource reporting manager changed: actorUserId={}, employeeId={}, managerEmployeeId={}",
                userId, employeeId, managerId);
    }

    @Transactional
    public void release(Long userId, Long employeeId) {
        lock(employeeId);
        Scope s = scopes.resolve(userId);
        requireTeamResource(s, employeeId);
        List<Map<String, Object>> previous = mappings.findByEmployeeIdIn(List.of(employeeId)).stream()
                .filter(row -> row.getReportingType() == null || "PRIMARY".equals(row.getReportingType()))
                .map(this::mappingSnapshot).toList();
        // Remove older PRIMARY rows too, so a superseded assignment cannot become active again.
        int removed = mappings.deletePrimaryResourceMappings(employeeId);
        recordAction(userId, s, employeeId, "RESOURCE_RELEASED", previous, List.of());
        log.info("Resource released: actorUserId={}, employeeId={}, primaryMappingsRemoved={}", userId, employeeId, removed);
    }

    private void recordAction(Long actorId, Scope scope, Long employeeId, String action,
                              List<Map<String, Object>> previous, List<Map<String, Object>> current) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("employeeId", employeeId);
        metadata.put("previousManagerEmployeeId", scope.parents().get(employeeId));
        metadata.put("previousMappings", previous);
        metadata.put("newMappings", current);
        auditTrail.record(AuditRecordRequest.builder()
                .moduleName("RESOURCE_MANAGEMENT").entityType("EMPLOYEE_REPORTING_MAPPING")
                .entityId(employeeId.toString()).actionType(action).actorUserId(actorId)
                .actorName(scope.people().get(scope.self()).getEmployeeName())
                .activitySummary(action + " for employee " + employeeId)
                .ipAddress(AuditRequestContext.ipAddress(null)).metadata(metadata).build());
    }

    private Map<String, Object> mappingSnapshot(EmployeeReportingMappingEntity mapping) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("mappingId", mapping.getMappingId());
        snapshot.put("managerEmployeeId", mapping.getManagerEmployeeId());
        snapshot.put("hodUserId", mapping.getHodUserId());
        snapshot.put("managerType", mapping.getManagerType());
        snapshot.put("projectId", mapping.getProjectId());
        snapshot.put("reportingType", mapping.getReportingType());
        return snapshot;
    }

    private void requireTeamResource(Scope scope, Long employeeId) {
        if (employeeId.equals(scope.self()) || !scope.team().contains(employeeId)) {
            throw error(HttpStatus.FORBIDDEN, "You can manage only resources assigned to you or your subordinates.");
        }
    }

    private void validateManager(Scope scope, Long employeeId, Long managerId) {
        if (managerId == null || !scope.team().contains(managerId)) {
            throw error(HttpStatus.FORBIDDEN, "Choose yourself or a subordinate in your reporting team as the reporting manager.");
        }
        Set<Long> visited = new HashSet<>();
        for (Long id = managerId; id != null && visited.add(id); id = scope.parents().get(id)) {
            if (id.equals(employeeId)) throw error(HttpStatus.CONFLICT, "This assignment would create a reporting cycle.");
        }
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
