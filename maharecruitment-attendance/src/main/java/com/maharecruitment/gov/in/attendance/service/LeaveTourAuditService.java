package com.maharecruitment.gov.in.attendance.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.maharecruitment.gov.in.attendance.entity.AttendanceRequestAuditLogEntity;
import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;
import com.maharecruitment.gov.in.attendance.entity.TourApplicationEntity;
import com.maharecruitment.gov.in.attendance.repository.AttendanceRequestAuditLogRepository;
import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Writes leave and tour request events to attendance_request_audit_log. Rows join the caller's transaction,
 * so a rolled-back decision leaves no audit row; the client IP is filled in by AuditIpEntityListener.
 */
@Service
@Transactional
public class LeaveTourAuditService {

    public static final String REQUEST_LEAVE = "LEAVE";
    public static final String REQUEST_TOUR = "TOUR";

    private static final String STATUS_PENDING = "PENDING";
    private static final String MOBILE_API_PREFIX = "/api/mobile/";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final AttendanceRequestAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveTourAuditService(
            AttendanceRequestAuditLogRepository auditLogRepository,
            UserRepository userRepository,
            EmployeeRepository employeeRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
    }

    public void leaveApplied(LeaveApplicationEntity leave) {
        record(leaveEvent(leave, "APPLY", null, null), employeeUserId(leave.getEmployeeId()), null,
                "Leave application submitted");
    }

    public void leaveDecided(LeaveApplicationEntity leave, String decision, String previousStage,
            String remarks, Long actorUserId) {
        String action = decisionAction(decision, leave.getStatus());
        record(leaveEvent(leave, action, STATUS_PENDING, previousStage), actorUserId, remarks,
                "Leave application " + decisionSummary(action));
    }

    public void leaveCancelled(LeaveApplicationEntity leave) {
        record(leaveEvent(leave, "CANCEL", STATUS_PENDING, leave.getApprovalStage()),
                employeeUserId(leave.getEmployeeId()), null, "Leave application cancelled by employee");
    }

    public void tourApplied(TourApplicationEntity tour) {
        record(tourEvent(tour, "APPLY", null, null), employeeUserId(tour.getEmployeeId()), null,
                "Tour application submitted");
    }

    public void tourDecided(TourApplicationEntity tour, String decision, String previousStage,
            String remarks, Long actorUserId) {
        String action = decisionAction(decision, tour.getStatus());
        record(tourEvent(tour, action, STATUS_PENDING, previousStage), actorUserId, remarks,
                "Tour application " + decisionSummary(action));
    }

    @Transactional(readOnly = true)
    public List<AttendanceRequestAuditLogEntity> getHistory(String requestType, Long requestId) {
        return auditLogRepository.findByRequestTypeAndRequestIdOrderByOccurredAtDescIdDesc(requestType, requestId);
    }

    private AttendanceRequestAuditLogEntity leaveEvent(LeaveApplicationEntity leave, String action,
            String previousStatus, String previousStage) {
        AttendanceRequestAuditLogEntity event = event(REQUEST_LEAVE, leave.getLeaveId(), leave.getEmployeeId(),
                action, previousStatus, leave.getStatus(), previousStage, leave.getApprovalStage());
        event.setRequestCategory(truncate(joinText(leave.getLeaveType(), leave.getLeaveCategory()), 100));
        event.setStartDate(leave.getStartDate());
        event.setEndDate(leave.getEndDate());
        return event;
    }

    private AttendanceRequestAuditLogEntity tourEvent(TourApplicationEntity tour, String action,
            String previousStatus, String previousStage) {
        AttendanceRequestAuditLogEntity event = event(REQUEST_TOUR, tour.getTourId(), tour.getEmployeeId(),
                action, previousStatus, tour.getStatus(), previousStage, tour.getApprovalStage());
        event.setRequestCategory(truncate(joinText(tour.getTourCategory(), tour.getTimePeriod()), 100));
        event.setStartDate(tour.getStartDate());
        event.setEndDate(tour.getEndDate());
        return event;
    }

    private AttendanceRequestAuditLogEntity event(String requestType, Long requestId, Long employeeId,
            String action, String previousStatus, String newStatus, String previousStage, String newStage) {
        AttendanceRequestAuditLogEntity event = new AttendanceRequestAuditLogEntity();
        event.setRequestType(requestType);
        event.setRequestId(requestId);
        event.setEmployeeId(employeeId);
        event.setActionType(action);
        event.setPreviousStatus(previousStatus);
        event.setNewStatus(newStatus);
        event.setPreviousStage(previousStage);
        event.setNewStage(newStage);
        return event;
    }

    private void record(AttendanceRequestAuditLogEntity event, Long actorUserId, String remarks, String summary) {
        User actor = actorUserId == null ? null : userRepository.findById(actorUserId).orElse(null);
        event.setActorUserId(actorUserId);
        if (actor != null) {
            event.setActorName(truncate(actor.getName(), 150));
            event.setActorLoginId(truncate(actor.getEmail(), 150));
            event.setActorRoles(truncate(roleNames(actor), 500));
            event.setActorDesignation(truncate(designation(actorUserId), 150));
        }
        event.setRemarks(truncate(trimToNull(remarks), 1000));

        HttpServletRequest request = currentRequest();
        event.setChannel(channel(request));
        event.setUserAgent(request == null ? null : truncate(trimToNull(request.getHeader("User-Agent")), 500));
        event.setDetails(truncate(details(event, summary), 2000));
        auditLogRepository.save(event);
    }

    // A manager approval on a two-level request only forwards it to the HOD; the request stays pending.
    private String decisionAction(String decision, String resultingStatus) {
        if ("REJECTED".equalsIgnoreCase(decision)) {
            return "REJECT";
        }
        return STATUS_PENDING.equalsIgnoreCase(resultingStatus) ? "FORWARD" : "APPROVE";
    }

    private String decisionSummary(String action) {
        return switch (action) {
            case "REJECT" -> "rejected";
            case "FORWARD" -> "recommended by reporting manager and forwarded to HOD";
            default -> "approved";
        };
    }

    private String details(AttendanceRequestAuditLogEntity event, String summary) {
        return summary + " (#" + event.getRequestId() + ") for employee ID " + event.getEmployeeId()
                + ", " + (event.getRequestCategory() == null ? "-" : event.getRequestCategory())
                + ", " + format(event.getStartDate()) + " to " + format(event.getEndDate())
                + ", status " + (event.getPreviousStatus() == null ? "-" : event.getPreviousStatus())
                + " -> " + event.getNewStatus()
                + (event.getRemarks() == null ? "" : ". Remarks: " + event.getRemarks());
    }

    private Long employeeUserId(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        return employeeRepository.findById(employeeId)
                .map(EmployeeEntity::getUser)
                .map(User::getId)
                .orElse(null);
    }

    private String designation(Long userId) {
        return employeeRepository.findByUser_Id(userId)
                .map(EmployeeEntity::getDesignation)
                .map(designation -> trimToNull(designation.getDesignationName()))
                .orElse(null);
    }

    private String roleNames(User user) {
        String roles = user.getRoles().stream()
                .map(Role::getName)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .sorted()
                .collect(Collectors.joining(","));
        return roles.isEmpty() ? null : roles;
    }

    private HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest()
                : null;
    }

    private String channel(HttpServletRequest request) {
        if (request == null) {
            return "SYSTEM";
        }
        String path = request.getRequestURI() == null ? "" : request.getRequestURI();
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        return path.startsWith(contextPath + MOBILE_API_PREFIX) ? "MOBILE" : "WEB";
    }

    private String joinText(String first, String second) {
        String joined = Stream.of(trimToNull(first), trimToNull(second))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" / "));
        return joined.isEmpty() ? null : joined;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String format(LocalDate date) {
        return date == null ? "-" : date.format(DATE_FORMAT);
    }
}
