package com.maharecruitment.gov.in.web.service.pip.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import com.maharecruitment.gov.in.audit.dto.AuditRecordRequest;
import com.maharecruitment.gov.in.audit.service.AuditTrailService;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeePipPlan;
import com.maharecruitment.gov.in.recruitment.entity.EmployeePipPlan.Status;
import com.maharecruitment.gov.in.recruitment.entity.EmployeePipReview;
import com.maharecruitment.gov.in.recruitment.repository.*;
import com.maharecruitment.gov.in.recruitment.service.organization.ResourceReportingScopeResolver;
import com.maharecruitment.gov.in.recruitment.service.organization.ResourceReportingScopeResolver.Scope;
import com.maharecruitment.gov.in.web.service.hr.EmployeeOffboardingService;
import com.maharecruitment.gov.in.web.service.pip.EmployeePipService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize(EmployeePipService.AUTHORIZATION)
public class EmployeePipServiceImpl implements EmployeePipService {
    private final UserRepository users;
    private final EmployeeRepository employees;
    private final ResourceReportingScopeResolver scopes;
    private final EmployeePipPlanRepository plans;
    private final EmployeePipReviewRepository reviews;
    private final AuditTrailService audit;
    private final EmployeeOffboardingService offboarding;

    @Override public Board board(String login, String search, int page) {
        if (page < 0 || (search != null && search.length() > 100)) throw bad("Invalid page or search (maximum 100 characters).");
        Scope scope = scopes.resolveIncludingInactive(actor(login).getId());
        Set<Long> team = new HashSet<>(scope.team());
        team.remove(scope.self());
        Set<Long> underReview = team.isEmpty() ? Set.of()
                : new HashSet<>(plans.findEmployeeIdsByStatus(team, Status.UNDER_REVIEW));
        List<EmployeeOption> options = team.stream().map(scope.people()::get)
                .filter(p -> "ACTIVE".equalsIgnoreCase(p.getEmployeeStatus())
                        && (p.getUserId() == null || Boolean.TRUE.equals(p.getUserActive()))
                        && !underReview.contains(p.getEmployeeId()))
                .map(p -> new EmployeeOption(p.getEmployeeId(), p.getEmployeeName(), p.getEmployeeCode()))
                .sorted(Comparator.comparing(EmployeeOption::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))).toList();
        String term = search == null ? "" : search.strip().toLowerCase(Locale.ROOT);
        List<Long> filtered = team.stream().filter(id -> {
            var person = scope.people().get(id);
            return (Objects.toString(person.getEmployeeName(), "") + " " + Objects.toString(person.getEmployeeCode(), ""))
                    .toLowerCase(Locale.ROOT).contains(term);
        }).toList();
        Page<PlanView> result = filtered.isEmpty() ? Page.empty(PageRequest.of(page, 12))
                : plans.findByEmployeeIdInOrderByCreatedDateTimeDescIdDesc(filtered, PageRequest.of(page, 12))
                        .map(p -> view(p, scope));
        return new Board(options, result);
    }

    @Override public Detail detail(String login, Long planId) {
        var plan = plans.findById(planId).orElseThrow(() -> missing());
        Scope scope = requireSubordinate(actor(login), plan.getEmployeeId());
        return new Detail(view(plan, scope), plan.getObjectives(),
                reviews.findByPlanIdOrderByReviewDateDescIdDesc(planId).stream()
                        .map(r -> new ReviewView(r.getReviewerName(), r.getReviewDate(), r.getRating(), r.getFeedback())).toList(),
                plan.getDecisionFeedback(), plan.getExitDate(),
                "ACTIVE".equalsIgnoreCase(scope.people().get(plan.getEmployeeId()).getEmployeeStatus())
                        && (scope.people().get(plan.getEmployeeId()).getUserId() == null
                            || Boolean.TRUE.equals(scope.people().get(plan.getEmployeeId()).getUserActive())), plan.getReason());
    }

    @Override @Transactional
    public Long start(String login, Long employeeId, LocalDate startDate, int months, String objectives, String reason) {
        if (employeeId == null || startDate == null || startDate.isAfter(today()) || (months != 1 && months != 2)) {
            throw bad("Select an employee, a start date no later than today, and a duration of one or two months.");
        }
        String goals = text(objectives, "Improvement objectives");
        String pipReason = text(reason, "Reason for PIP");
        User actor = actor(login);
        var employee = employees.lockForExit(employeeId).orElseThrow(() -> missing());
        requireSubordinate(actor, employeeId);
        if (!"ACTIVE".equalsIgnoreCase(employee.getStatus())
                || (employee.getUser() != null && !Boolean.TRUE.equals(employee.getUser().getActive()))) {
            throw bad("Only active employees can be placed under review.");
        }
        if (plans.existsByEmployeeIdAndStatus(employeeId, Status.UNDER_REVIEW)) {
            throw conflict("This employee already has an active PIP.");
        }
        var plan = new EmployeePipPlan();
        plan.setEmployeeId(employeeId);
        plan.setCreatedByUserId(actor.getId());
        plan.setStartDate(startDate);
        plan.setDurationMonths(months);
        plan.setEndDate(startDate.plusMonths(months).minusDays(1));
        plan.setObjectives(goals);
        plan.setReason(pipReason);
        plan = plans.save(plan);
        log(actor, plan, "PIP_STARTED", Map.of("durationMonths", months, "startDate", startDate.toString(),
                "endDate", plan.getEndDate().toString(), "objectives", goals, "reason", pipReason));
        return plan.getId();
    }

    @Override @Transactional
    public void review(String login, Long planId, LocalDate reviewDate, int rating, String feedback) {
        User actor = actor(login);
        var plan = lockedPlan(actor, planId);
        requireOpen(plan);
        String notes = text(feedback, "Performance feedback");
        if (reviewDate == null || reviewDate.isBefore(plan.getStartDate()) || reviewDate.isAfter(today())
                || rating < 1 || rating > 5) throw bad("Use a review date from the PIP start through today and a rating from 1 to 5.");
        var review = new EmployeePipReview();
        review.setPlanId(planId);
        review.setReviewerUserId(actor.getId());
        review.setReviewerName(actor.getName());
        review.setReviewDate(reviewDate);
        review.setRating(rating);
        review.setFeedback(notes);
        review = reviews.save(review);
        log(actor, plan, "PIP_REVIEW_ADDED", Map.of("reviewId", review.getId(), "reviewDate", reviewDate.toString(),
                "rating", rating, "feedback", notes));
    }

    @Override @Transactional
    public void decide(String login, Long planId, Decision decision, String feedback, LocalDate exitDate) {
        User actor = actor(login);
        var plan = lockedPlan(actor, planId);
        requireOpen(plan);
        String notes = text(feedback, "Decision feedback");
        if (decision == null) throw bad("Select an outcome.");
        if (decision == Decision.EXIT) {
            if (exitDate == null || exitDate.isBefore(plan.getStartDate()) || exitDate.isAfter(today())) {
                throw bad("Exit date must be between the PIP start date and today.");
            }
            // Reuses the same atomic account deactivation and vacancy release as HR exit.
            offboarding.exitEmployee(plan.getEmployeeId(), exitDate, notes.substring(0, Math.min(notes.length(), 1000)),
                    login, EmployeeOffboardingService.Source.PIP);
            plan.setExitDate(exitDate);
            plan.setStatus(Status.EXIT_RECORDED);
        } else {
            plan.setStatus(Status.COMPLETED);
        }
        plan.setDecisionByUserId(actor.getId());
        plan.setDecisionDate(today());
        plan.setDecisionFeedback(notes);
        plans.save(plan);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("previousStatus", Status.UNDER_REVIEW.name());
        metadata.put("newStatus", plan.getStatus().name());
        metadata.put("feedback", notes);
        metadata.put("exitDate", exitDate);
        log(actor, plan, decision == Decision.CONTINUE ? "PIP_COMPLETED" : "PIP_EXIT_RECORDED", metadata);
    }

    private EmployeePipPlan lockedPlan(User actor, Long id) {
        var snapshot = plans.findById(id).orElseThrow(() -> missing());
        var employee = employees.lockForExit(snapshot.getEmployeeId()).orElseThrow(() -> missing());
        requireSubordinate(actor, employee.getEmployeeId());
        if (!"ACTIVE".equalsIgnoreCase(employee.getStatus())
                || (employee.getUser() != null && !Boolean.TRUE.equals(employee.getUser().getActive()))) {
            throw conflict("The employee is no longer active.");
        }
        return plans.lockById(id).orElseThrow(() -> missing());
    }

    private Scope requireSubordinate(User actor, Long employeeId) {
        Scope scope = scopes.resolveIncludingInactive(actor.getId());
        if (employeeId.equals(scope.self()) || !scope.team().contains(employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can manage PIP only for employees in your reporting team.");
        }
        return scope;
    }

    private User actor(String login) {
        if (!StringUtils.hasText(login)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findByEmailIgnoreCaseAndActiveTrue(login).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
    }
    private PlanView view(EmployeePipPlan plan, Scope scope) {
        var person = scope.people().get(plan.getEmployeeId());
        return new PlanView(plan.getId(), plan.getEmployeeId(), person.getEmployeeName(), person.getEmployeeCode(),
                plan.getStartDate(), plan.getEndDate(), plan.getDurationMonths(), plan.getStatus());
    }
    private void requireOpen(EmployeePipPlan plan) {
        if (plan.getStatus() != Status.UNDER_REVIEW) throw conflict("This PIP has already been closed. Refresh the page.");
    }
    private void log(User actor, EmployeePipPlan plan, String action, Map<String, Object> details) {
        Map<String, Object> metadata = new LinkedHashMap<>(details);
        metadata.put("employeeId", plan.getEmployeeId());
        audit.record(AuditRecordRequest.builder().moduleName("EMPLOYEE_PIP").entityType("PIP_PLAN")
                .entityId(plan.getId().toString()).actionType(action).actorUserId(actor.getId())
                .actorLoginId(actor.getEmail()).actorName(actor.getName())
                .activitySummary(action.replace('_', ' ')).metadata(metadata).build());
    }
    private String text(String value, String label) {
        if (!StringUtils.hasText(value) || value.length() > 2000) throw bad(label + " is required (maximum 2,000 characters).");
        return value.strip();
    }
    private LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Kolkata")); }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "PIP or employee not found."); }
    private ResponseStatusException bad(String reason) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason); }
    private ResponseStatusException conflict(String reason) { return new ResponseStatusException(HttpStatus.CONFLICT, reason); }
}
