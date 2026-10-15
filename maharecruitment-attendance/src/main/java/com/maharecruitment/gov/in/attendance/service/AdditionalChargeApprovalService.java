package com.maharecruitment.gov.in.attendance.service;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.maharecruitment.gov.in.recruitment.repository.AdditionalChargeRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeHierarchyRepository;
import com.maharecruitment.gov.in.recruitment.service.ReportingManagerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A request-scoped view of active coverage; original application approver IDs are never rewritten. */
@Service
@Transactional(readOnly = true)
public class AdditionalChargeApprovalService {
    private final AdditionalChargeRepository charges;
    private final EmployeeHierarchyRepository hierarchy;
    private final ReportingManagerService reporting;

    public AdditionalChargeApprovalService(AdditionalChargeRepository charges, EmployeeHierarchyRepository hierarchy,
                                          ReportingManagerService reporting) {
        this.charges = charges;
        this.hierarchy = hierarchy;
        this.reporting = reporting;
    }

    public Snapshot snapshot() {
        var active = charges.activeApprovalCharges(LocalDate.now());
        if (active.isEmpty()) return Snapshot.empty();
        var employees = hierarchy.findActiveEmployees().stream().collect(Collectors.toMap(
                EmployeeHierarchyRepository.EmployeeRow::getEmployeeId, Function.identity()));
        Map<Long, Long> employeeByUser = new HashMap<>();
        employees.values().forEach(e -> { if (e.getUserId() != null) employeeByUser.put(e.getUserId(), e.getEmployeeId()); });
        Map<Long, List<Long>> children = new HashMap<>();
        Set<Long> assigned = new HashSet<>();
        for (var mapping : hierarchy.findReportingRelationships("PRIMARY")) {
            if (!assigned.add(mapping.getEmployeeId())) continue;
            Long parent = mapping.getManagerEmployeeId() != null ? mapping.getManagerEmployeeId()
                    : employeeByUser.get(mapping.getHodUserId());
            if (parent != null) children.computeIfAbsent(parent, unused -> new ArrayList<>()).add(mapping.getEmployeeId());
        }
        Set<Long> cellIds = active.stream().map(AdditionalChargeRepository.ApprovalCharge::getCellId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Long> cellByEmployee = new HashMap<>();
        if (!cellIds.isEmpty()) charges.approvalCellMembers(cellIds).forEach(e -> cellByEmployee.put(e.getEmployeeId(), e.getCellId()));
        Map<Long, List<AdditionalChargeRepository.ApprovalCharge>> byCharge = new LinkedHashMap<>();
        active.forEach(a -> byCharge.computeIfAbsent(a.getChargeId(), unused -> new ArrayList<>()).add(a));
        Map<Authority, Long> transfers = new HashMap<>();
        for (var members : byCharge.values()) {
            var charge = members.getFirst();
            if (!employees.containsKey(charge.getHolderEmployeeId()) || charge.getHolderUserId() == null
                    || charge.getSourceUserId() == null || charge.getHolderUserId().equals(charge.getSourceUserId())) continue;
            Set<Long> visited = new HashSet<>();
            ArrayDeque<Long> queue = new ArrayDeque<>();
            // A root moved to another cell is no longer covered, including its branch.
            members.stream().map(AdditionalChargeRepository.ApprovalCharge::getEmployeeId)
                    .filter(id -> charge.getCellId() == null || Objects.equals(cellByEmployee.get(id), charge.getCellId())).forEach(queue::add);
            while (!queue.isEmpty()) {
                Long employeeId = queue.remove();
                if (!visited.add(employeeId)) continue;
                var employee = employees.get(employeeId);
                if (employee != null && (charge.getCellId() == null || Objects.equals(cellByEmployee.get(employeeId), charge.getCellId()))
                        && !Objects.equals(employee.getUserId(), charge.getHolderUserId())
                        && !Objects.equals(employeeId, charge.getHolderEmployeeId())) {
                    transfers.putIfAbsent(new Authority(employeeId, charge.getSourceUserId()), charge.getHolderUserId());
                }
                queue.addAll(children.getOrDefault(employeeId, List.of()));
            }
        }
        return new Snapshot(transfers, reporting::getEffectiveEmployeeIdsForAuthority);
    }

    private record Authority(Long employeeId, Long sourceUserId) { }

    public static final class Snapshot {
        private final Map<Authority, Long> transfers;
        private final Map<Long, Map<Long, List<Long>>> incoming = new HashMap<>();
        private final Set<Long> affected = new HashSet<>();
        private final Function<Long, List<Long>> legacyEmployees;
        private final Map<Long, Set<Long>> legacyCache = new HashMap<>();

        private Snapshot(Map<Authority, Long> transfers, Function<Long, List<Long>> legacyEmployees) {
            this.transfers = Map.copyOf(transfers);
            this.legacyEmployees = legacyEmployees;
            transfers.forEach((authority, holder) -> {
                affected.add(authority.sourceUserId());
                affected.add(holder);
                incoming.computeIfAbsent(holder, unused -> new HashMap<>())
                        .computeIfAbsent(authority.employeeId(), unused -> new ArrayList<>()).add(authority.sourceUserId());
            });
        }

        public static Snapshot empty() { return new Snapshot(Map.of(), unused -> List.of()); }
        public boolean affects(Long actor) { return actor != null && affected.contains(actor); }
        public List<Long> incomingEmployeeIds(Long actor) {
            return new ArrayList<>(incoming.getOrDefault(actor, Map.of()).keySet());
        }
        public boolean replaces(Long employeeId, Long originalApprover) {
            return originalApprover != null && transfers.containsKey(new Authority(employeeId, originalApprover));
        }
        public Long effectiveApprover(Long employeeId, Long originalApprover) {
            return originalApprover == null ? null : transfers.getOrDefault(new Authority(employeeId, originalApprover), originalApprover);
        }
        public boolean canAct(Long actor, Long employeeId, String stage, Long manager, Long hod) {
            Long original = "MANAGER".equalsIgnoreCase(stage) ? manager : "HOD".equalsIgnoreCase(stage) ? hod : null;
            return actor != null && original != null && actor.equals(effectiveApprover(employeeId, original));
        }
        public boolean canActLegacy(Long actor, Long employeeId, Collection<Long> ownEmployees) {
            if (actor == null || employeeId == null) return false;
            if (ownEmployees.contains(employeeId) && actor.equals(effectiveApprover(employeeId, actor))) return true;
            for (Long source : incoming.getOrDefault(actor, Map.of()).getOrDefault(employeeId, List.of())) {
                // Legacy approvals still require the incumbent's actual authority, not just hierarchy membership.
                if (legacyCache.computeIfAbsent(source, id -> new HashSet<>(legacyEmployees.apply(id))).contains(employeeId)) return true;
            }
            return false;
        }
    }
}
