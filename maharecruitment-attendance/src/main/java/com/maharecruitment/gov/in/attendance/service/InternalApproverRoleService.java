package com.maharecruitment.gov.in.attendance.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.maharecruitment.gov.in.attendance.dto.InternalDecisionLogView;
import com.maharecruitment.gov.in.attendance.entity.InternalApproverRoleAuditLogEntity;
import com.maharecruitment.gov.in.attendance.entity.InternalApproverRoleEntity;
import com.maharecruitment.gov.in.attendance.repository.AttendanceRequestAuditLogRepository;
import com.maharecruitment.gov.in.attendance.repository.InternalApproverRoleAuditLogRepository;
import com.maharecruitment.gov.in.attendance.repository.InternalApproverRoleRepository;
import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.RoleRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;

/**
 * Maintains which roles may decide INTERNAL employees' leave/tour requests (Admin > Internal Approver Roles)
 * and logs every change with who made it, when, from which IP and why.
 */
@Service
@Transactional(readOnly = true)
public class InternalApproverRoleService {

    public static final String ACTION_UPDATED = "UPDATED";
    static final int MAX_REMARKS = 500;
    private static final int MAX_PAGE_SIZE = 50;

    private final InternalApproverRoleRepository approverRoleRepository;
    private final InternalApproverRoleAuditLogRepository changeLogRepository;
    private final AttendanceRequestAuditLogRepository requestAuditLogRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public InternalApproverRoleService(
            InternalApproverRoleRepository approverRoleRepository,
            InternalApproverRoleAuditLogRepository changeLogRepository,
            AttendanceRequestAuditLogRepository requestAuditLogRepository,
            RoleRepository roleRepository,
            UserRepository userRepository) {
        this.approverRoleRepository = approverRoleRepository;
        this.changeLogRepository = changeLogRepository;
        this.requestAuditLogRepository = requestAuditLogRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    /** A role from the roles table and whether it may currently decide internal employees' requests. */
    public record RoleOption(Long roleId, String roleName, String shortName, boolean selected) {
    }

    /** Role names added and removed by a save; both empty when nothing changed. */
    public record ChangeResult(List<String> added, List<String> removed) {
        public boolean changed() {
            return !added.isEmpty() || !removed.isEmpty();
        }
    }

    public List<RoleOption> getRoleOptions() {
        Set<Long> selected = approverRoleRepository.findAll().stream()
                .map(InternalApproverRoleEntity::getRoleId)
                .collect(Collectors.toSet());
        return roleRepository.findAllByOrderByNameAsc().stream()
                .map(role -> new RoleOption(role.getId(), role.getName(),
                        InternalEmployeeApprovalPolicy.shortName(role.getName()), selected.contains(role.getId())))
                .toList();
    }

    public Page<InternalApproverRoleAuditLogEntity> getChangeLog(int page, int size) {
        return changeLogRepository.findAllByOrderByChangedAtDescIdDesc(pageRequest(page, size));
    }

    public Page<InternalDecisionLogView> getInternalDecisionLog(int page, int size) {
        return requestAuditLogRepository.findInternalEmployeeDecisions(pageRequest(page, size));
    }

    /**
     * Replaces the approver roles with {@code roleIds}. Writes one change-log row when something changed;
     * saving the same selection again changes and logs nothing.
     */
    @Transactional
    public ChangeResult updateApproverRoles(Collection<Long> roleIds, String remarks, String actorLoginId) {
        Set<Long> requestedIds = roleIds == null ? Set.of() : roleIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requestedIds.isEmpty()) {
            throw new IllegalArgumentException("Select at least one role. Without one, nobody can approve leave"
                    + " or tour requests of internal employees.");
        }
        String note = StringUtils.hasText(remarks) ? remarks.trim() : null;
        if (note != null && note.length() > MAX_REMARKS) {
            throw new IllegalArgumentException("Reason for change must be " + MAX_REMARKS + " characters or fewer.");
        }

        Map<Long, Role> requested = rolesById(requestedIds);
        if (requested.size() != requestedIds.size()) {
            throw new IllegalArgumentException("A selected role no longer exists. Reload the page and try again.");
        }
        Set<Long> currentIds = approverRoleRepository.findAll().stream()
                .map(InternalApproverRoleEntity::getRoleId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Role> current = rolesById(currentIds);

        List<Long> addedIds = requestedIds.stream().filter(id -> !currentIds.contains(id)).toList();
        List<Long> removedIds = currentIds.stream().filter(id -> !requestedIds.contains(id)).toList();
        ChangeResult result = new ChangeResult(names(addedIds, requested), names(removedIds, current));
        if (!result.changed()) {
            return result;
        }

        User actor = StringUtils.hasText(actorLoginId)
                ? userRepository.findByEmailIgnoreCase(actorLoginId.trim()).orElse(null)
                : null;
        Long actorUserId = actor == null ? null : actor.getId();
        if (!removedIds.isEmpty()) {
            approverRoleRepository.deleteAllByIdInBatch(removedIds);
        }
        if (!addedIds.isEmpty()) {
            approverRoleRepository.saveAll(addedIds.stream()
                    .map(id -> new InternalApproverRoleEntity(id, actorUserId))
                    .toList());
        }

        InternalApproverRoleAuditLogEntity entry = new InternalApproverRoleAuditLogEntity();
        entry.setActionType(ACTION_UPDATED);
        entry.setRolesAdded(joined(result.added()));
        entry.setRolesRemoved(joined(result.removed()));
        entry.setRolesBefore(joined(names(currentIds, current)));
        entry.setRolesAfter(joined(names(requestedIds, requested)));
        entry.setRemarks(note);
        entry.setActorUserId(actorUserId);
        entry.setActorName(actor == null ? null : truncate(actor.getName(), 150));
        entry.setActorLoginId(truncate(actor == null ? actorLoginId : actor.getEmail(), 150));
        entry.setUserAgent(truncate(userAgent(), 500));
        changeLogRepository.save(entry);
        return result;
    }

    private Map<Long, Role> rolesById(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return roleRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Role::getId, Function.identity()));
    }

    private static List<String> names(Collection<Long> ids, Map<Long, Role> roles) {
        return ids.stream()
                .map(roles::get)
                .filter(Objects::nonNull)
                .map(Role::getName)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private static String joined(List<String> names) {
        return names.isEmpty() ? null : truncate(String.join(",", names), 1000);
    }

    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }

    private static String userAgent() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest().getHeader("User-Agent")
                : null;
    }

    private static String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
