package com.maharecruitment.gov.in.recruitment.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.maharecruitment.gov.in.master.repository.CellMasterRepository;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import java.util.List;
import java.util.Objects;
import com.maharecruitment.gov.in.recruitment.entity.AdditionalCharge;
import com.maharecruitment.gov.in.recruitment.repository.AdditionalChargeRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdditionalChargeService {
    private final AdditionalChargeRepository repository;
    private final EmployeeRepository employees;
    private final CellMasterRepository cells;

    public AdditionalChargeService(AdditionalChargeRepository repository, EmployeeRepository employees,
                                  CellMasterRepository cells) {
        this.repository = repository;
        this.employees = employees;
        this.cells = cells;
    }

    public record Request(
            @NotBlank(message = "Select an employee or user.")
            @Pattern(regexp = "[EU]:[1-9][0-9]*", message = "Select a valid employee or user.") String assignee,
            @NotBlank(message = "Select the role for the additional charge.")
            @Size(max = 100, message = "Position must be at most 100 characters.") String position,
            @Pattern(regexp = "|C:[1-9][0-9]*", message = "Select a valid cell.") String scope,
            @NotNull(message = "Select a start date.") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Size(max = 150, message = "Order reference must be at most 150 characters.") String orderReference,
            @Size(max = 1000, message = "Responsibilities must be at most 1000 characters.") String responsibilities,
            @Pattern(regexp = "|[EU]:[1-9][0-9]*", message = "Select a valid person to replace.") String replaces) {
        public Request(String assignee, String position, String scope, LocalDate startDate, LocalDate endDate,
                       String orderReference, String responsibilities) {
            this(assignee, position, scope, startDate, endDate, orderReference, responsibilities, "");
        }
    }

    public record Entry(Long id, String assignee, String position, String scope, LocalDate startDate,
                        LocalDate endDate, String orderReference, String responsibilities,
                        String status, String assignedBy, String revokedBy, String replaces, int teamSize) { }

    public Page<Entry> list(int page) {
        LocalDate today = LocalDate.now();
        return repository.findAllByOrderByIdDesc(PageRequest.of(Math.max(0, page), 20)).map(a -> new Entry(
                a.getId(), a.getEmployee() != null ? a.getEmployee().getFullName() : a.getUser().getName(),
                a.getPosition(), a.getDepartment() != null ? a.getDepartment().getDepartmentName()
                        : a.getCell() != null ? a.getCell().getCellName() : "All cells",
                a.getStartDate(), a.getEndDate(), a.getOrderReference(), a.getResponsibilities(),
                a.getRevokedAt() != null ? "Revoked" : a.getStartDate().isAfter(today) ? "Scheduled"
                        : a.getEndDate() != null && a.getEndDate().isBefore(today) ? "Expired" : "Active",
                a.getAssignedBy(), a.getRevokedBy(),
                a.getReplacedEmployee() != null ? a.getReplacedEmployee().getFullName()
                        : a.getReplacedUser() != null ? a.getReplacedUser().getName() : "",
                a.getTeamEmployeeIds().size()));
    }

    /** The caller supplies the authenticated login, never a requested employee identity. */
    public List<AdditionalChargeRepository.PersonalCharge> personalActiveCharges(String loginEmail) {
        if (loginEmail == null || loginEmail.isBlank()) return List.of();
        return repository.personalActiveCharges(loginEmail, LocalDate.now());
    }

    @Transactional
    public void assign(Request request, String actor) {
        if (request.startDate() == null || (request.endDate() != null && request.endDate().isBefore(request.startDate()))) {
            throw new IllegalArgumentException("End date must be on or after the start date.");
        }
        AdditionalCharge charge = new AdditionalCharge();
        Long assigneeId = parseId(request.assignee(), "[EU]:[1-9][0-9]*", "Select an employee or user.");
        if (request.assignee().startsWith("U:")) {
            // Resolve linked accounts to the employee identity so the same person cannot be assigned twice.
            assigneeId = employees.findByUser_Id(assigneeId).map(e -> e.getEmployeeId()).orElse(null);
        }
        var source = request.replaces() == null || request.replaces().isBlank() ? null : replacement(request.replaces());
        // Lock both identities in a consistent order. Locking the incumbent also serializes
        // all-cell and cell-specific replacements for that person before overlap validation.
        int sourceOrder = source == null ? 0 : compareSource(source, assigneeId,
                assigneeId == null ? parseId(request.assignee(), "U:[1-9][0-9]*", "Select a user.") : null);
        if (source != null && sourceOrder == 0) throw new IllegalArgumentException("The charge holder and person being replaced must be different people.");
        if (source != null && sourceOrder < 0) lockReplacement(source);
        if (assigneeId != null) {
            var employee = repository.lockEmployee(assigneeId)
                    .orElseThrow(() -> new IllegalArgumentException("Selected employee was not found."));
            if (!"ACTIVE".equalsIgnoreCase(employee.getStatus())
                    || (employee.getUser() != null && !Boolean.TRUE.equals(employee.getUser().getActive()))
                    || !repository.eligibleEmployee(employee.getEmployeeId())) {
                throw new IllegalArgumentException("Select an active employee who has not been relieved.");
            }
            charge.setEmployee(employee);
        } else {
            var user = repository.lockUser(parseId(request.assignee(), "U:[1-9][0-9]*", "Select a user."))
                    .orElseThrow(() -> new IllegalArgumentException("Selected user was not found."));
            if (!Boolean.TRUE.equals(user.getActive())) throw new IllegalArgumentException("Select an active user.");
            charge.setUser(user);
        }
        if (source != null && sourceOrder > 0) lockReplacement(source);
        Long scopeId = request.scope() == null || request.scope().isBlank() ? null
                : parseId(request.scope(), "C:[1-9][0-9]*", "Select a valid cell.");
        if (scopeId != null) {
            var cell = repository.lockCell(scopeId)
                    .orElseThrow(() -> new IllegalArgumentException("Selected cell was not found."));
            if (!"Y".equalsIgnoreCase(cell.getActiveFlag()) || cell.getWing() == null
                    || !"Y".equalsIgnoreCase(cell.getWing().getActiveFlag())) {
                throw new IllegalArgumentException("Select an active cell in an active wing.");
            }
            charge.setCell(cell);
        }
        if (source != null) {
            Long targetEmployeeId = charge.getEmployee() == null ? null : charge.getEmployee().getEmployeeId();
            Long targetUserId = charge.getUser() != null ? charge.getUser().getId()
                    : charge.getEmployee().getUser() != null ? charge.getEmployee().getUser().getId() : null;
            if ((targetEmployeeId != null && Objects.equals(targetEmployeeId, source.employeeId()))
                    || (targetUserId != null && Objects.equals(targetUserId, source.userId()))) {
                throw new IllegalArgumentException("The charge holder and person being replaced must be different people.");
            }
            if (repository.replacementOverlaps(scopeId, source.employeeId(), source.userId(),
                    request.startDate(), request.endDate() == null ? LocalDate.of(9999, 12, 31) : request.endDate())) {
                throw new IllegalArgumentException("This person already has replacement coverage for overlapping cells and dates.");
            }
            charge.setReplacedEmployee(source.employee());
            charge.setReplacedUser(source.user());
            for (var member : repository.replacementTeam(scopeId, source.employeeId(), source.userId())) {
                if (!Objects.equals(member.getId(), targetEmployeeId)) charge.getTeamEmployeeIds().add(member.getId());
            }
            if (!charge.getTeamEmployeeIds().isEmpty()
                    && (charge.getEmployee() == null || charge.getEmployee().getUser() == null)) {
                throw new IllegalArgumentException("Link the selected user to an active employee record and user account before mapping a replacement team and approvals.");
            }
        }
        String position = request.position() == null ? "" : request.position().trim();
        if (position.isEmpty() || position.length() > 100) throw new IllegalArgumentException("Select a valid role for the additional charge.");
        position = repository.findChargeRole(position)
                .orElseThrow(() -> new IllegalArgumentException("Selected role is no longer available. Select a role from the list."));
        if (repository.overlaps(charge.getEmployee() == null ? null : charge.getEmployee().getEmployeeId(),
                charge.getUser() == null ? null : charge.getUser().getId(), position,
                null, scopeId,
                request.startDate(), request.endDate() == null ? LocalDate.of(9999, 12, 31) : request.endDate())) {
            throw new IllegalArgumentException("This person already has this charge for overlapping dates in the selected scope.");
        }
        charge.setPosition(position);
        charge.setStartDate(request.startDate());
        charge.setEndDate(request.endDate());
        charge.setOrderReference(request.orderReference() == null ? "" : request.orderReference().trim());
        charge.setResponsibilities(request.responsibilities() == null ? "" : request.responsibilities().trim());
        charge.setAssignedBy(actor);
        repository.save(charge);
    }

    public List<AdditionalChargeRepository.Choice> previewTeam(String scope, String replaces) {
        Long cellId = scope == null || scope.isBlank() ? null : parseId(scope, "C:[1-9][0-9]*", "Select a cell.");
        if (cellId != null) {
            var cell = cells.findByCellId(cellId).orElseThrow(() -> new IllegalArgumentException("Selected cell was not found."));
            if (!"Y".equalsIgnoreCase(cell.getActiveFlag()) || cell.getWing() == null
                    || !"Y".equalsIgnoreCase(cell.getWing().getActiveFlag())) {
                throw new IllegalArgumentException("Select an active cell in an active wing.");
            }
        }
        var source = replacement(replaces);
        return repository.replacementTeam(cellId, source.employeeId(), source.userId());
    }

    private int compareSource(Replacement source, Long employeeId, Long userId) {
        if (source.employeeId() != null && employeeId == null) return -1;
        if (source.employeeId() == null && employeeId != null) return 1;
        return Long.compare(source.employeeId() != null ? source.employeeId() : source.userId(),
                employeeId != null ? employeeId : userId);
    }

    private void lockReplacement(Replacement source) {
        if (source.employeeId() != null) {
            repository.lockEmployee(source.employeeId())
                    .orElseThrow(() -> new IllegalArgumentException("Person being replaced was not found."));
        } else {
            repository.lockUser(source.userId())
                    .orElseThrow(() -> new IllegalArgumentException("User being replaced was not found."));
        }
    }

    private Replacement replacement(String value) {
        Long id = parseId(value, "[EU]:[1-9][0-9]*", "Select a person to replace.");
        if (value.startsWith("E:")) {
            return new Replacement(employees.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Person being replaced was not found.")), null);
        }
        var linked = employees.findByUser_Id(id);
        if (linked.isPresent()) return new Replacement(linked.get(), null);
        return new Replacement(null, repository.replacementUser(id)
                .orElseThrow(() -> new IllegalArgumentException("User being replaced was not found.")));
    }

    private record Replacement(EmployeeEntity employee, User user) {
        Long employeeId() { return employee == null ? null : employee.getEmployeeId(); }
        Long userId() { return user != null ? user.getId() : employee.getUser() == null ? null : employee.getUser().getId(); }
    }

    @Transactional
    public void revoke(Long id, String actor) {
        var charge = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Charge was not found."));
        if (charge.getRevokedAt() != null) throw new IllegalArgumentException("This charge has already been revoked.");
        charge.setRevokedAt(LocalDateTime.now());
        charge.setRevokedBy(actor);
        repository.save(charge);
    }

    private static Long parseId(String value, String pattern, String message) {
        if (value == null || !value.matches(pattern)) throw new IllegalArgumentException(message);
        try { return Long.valueOf(value.substring(2)); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException(message); }
    }
}
