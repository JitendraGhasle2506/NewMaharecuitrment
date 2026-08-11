package com.maharecruitment.gov.in.web.service.mobile.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.attendance.dto.LeaveApplicationHODDTO;
import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;
import com.maharecruitment.gov.in.attendance.repository.LeaveApplicationRepository;
import com.maharecruitment.gov.in.attendance.service.LeaveApplicationService;
import com.maharecruitment.gov.in.master.entity.LeaveEntity;
import com.maharecruitment.gov.in.master.repository.LeaveRepository;
import com.maharecruitment.gov.in.recruitment.service.ReportingManagerService;
import com.maharecruitment.gov.in.web.dto.mobile.MobileCompOffValidationResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLeaveApplication;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLeaveApplicationResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLeaveApplyRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLeaveApprovalsResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLeaveHistoryResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLeaveOptionsResponse;
import com.maharecruitment.gov.in.web.service.mobile.MobileApiException;
import com.maharecruitment.gov.in.web.service.mobile.MobileEmployeeAccessContext;
import com.maharecruitment.gov.in.web.service.mobile.MobileEmployeeAccessService;
import com.maharecruitment.gov.in.web.service.mobile.MobileLeaveService;

@Service
public class MobileLeaveServiceImpl implements MobileLeaveService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String ROLE_HOD = "ROLE_HOD";
    private static final String COMP_OFF_CODE = "CO";

    private static final List<MobileLeaveOptionsResponse.LeaveCategory> LEAVE_CATEGORIES = List.of(
            new MobileLeaveOptionsResponse.LeaveCategory("FULL_DAY", "Full Day"),
            new MobileLeaveOptionsResponse.LeaveCategory("FIRST_HALF", "First Half"),
            new MobileLeaveOptionsResponse.LeaveCategory("SECOND_HALF", "Second Half"));

    private final MobileEmployeeAccessService mobileEmployeeAccessService;
    private final LeaveApplicationService leaveApplicationService;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveRepository leaveRepository;
    private final ReportingManagerService reportingManagerService;

    public MobileLeaveServiceImpl(
            MobileEmployeeAccessService mobileEmployeeAccessService,
            LeaveApplicationService leaveApplicationService,
            LeaveApplicationRepository leaveApplicationRepository,
            LeaveRepository leaveRepository,
            ReportingManagerService reportingManagerService) {
        this.mobileEmployeeAccessService = mobileEmployeeAccessService;
        this.leaveApplicationService = leaveApplicationService;
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.leaveRepository = leaveRepository;
        this.reportingManagerService = reportingManagerService;
    }

    @Override
    @Transactional(readOnly = true)
    public MobileLeaveOptionsResponse getOptions(Long employeeId) {
        Long currentEmployeeId = mobileEmployeeAccessService.requireCurrentActiveEmployee(employeeId).getEmployeeId();
        return new MobileLeaveOptionsResponse(
                true,
                "Leave application options fetched successfully.",
                currentEmployeeId,
                getLeaveTypes(),
                LEAVE_CATEGORIES);
    }

    @Override
    @Transactional
    public MobileLeaveApplicationResponse apply(MobileLeaveApplyRequest request) {
        Long employeeId = mobileEmployeeAccessService.requireCurrentActiveEmployee(request.employeeId()).getEmployeeId();

        LeaveApplicationEntity leave = new LeaveApplicationEntity();
        leave.setEmployeeId(employeeId);
        leave.setLeaveType(resolveLeaveType(request.leaveType()));
        leave.setLeaveCategory(resolveLeaveCategory(request.leaveCategory()));
        leave.setStartDate(request.startDate());
        leave.setEndDate(request.endDate());
        leave.setCompOffWorkDate(request.compOffWorkDate());
        leave.setDescription(textOrNull(request.description()));

        try {
            leaveApplicationService.saveLeaveApplication(leave);
        } catch (IllegalArgumentException ex) {
            throw badRequest("INVALID_LEAVE_APPLICATION", ex.getMessage());
        }

        return new MobileLeaveApplicationResponse(
                true,
                "Leave application submitted successfully.",
                toApplication(leave));
    }

    @Override
    @Transactional(readOnly = true)
    public MobileLeaveHistoryResponse getApplications(Long employeeId) {
        Long currentEmployeeId = mobileEmployeeAccessService.requireCurrentActiveEmployee(employeeId).getEmployeeId();
        List<MobileLeaveApplication> applications = leaveApplicationService
                .getLeaveApplicationsByEmployee(currentEmployeeId)
                .stream()
                .map(this::toApplication)
                .toList();
        return new MobileLeaveHistoryResponse(
                true,
                "Leave applications fetched successfully.",
                currentEmployeeId,
                applications);
    }

    @Override
    @Transactional(readOnly = true)
    public MobileCompOffValidationResponse validateCompOffWorkedDate(Long employeeId, LocalDate workedDate) {
        Long currentEmployeeId = mobileEmployeeAccessService.requireCurrentActiveEmployee(employeeId).getEmployeeId();
        if (workedDate == null) {
            throw badRequest("WORKED_DATE_REQUIRED", "Comp-off worked date is required.");
        }
        boolean valid = !workedDate.isAfter(LocalDate.now())
                && leaveApplicationService.isValidCompOffWorkedDate(currentEmployeeId, workedDate);
        return new MobileCompOffValidationResponse(
                true,
                valid
                        ? "Worked date verified."
                        : "Comp-off worked date is allowed only for a non-future date when you were present or had an approved tour.",
                currentEmployeeId,
                workedDate,
                valid);
    }

    @Override
    @Transactional
    public MobileLeaveApplicationResponse cancel(Long employeeId, Long leaveId) {
        Long currentEmployeeId = mobileEmployeeAccessService.requireCurrentActiveEmployee(employeeId).getEmployeeId();
        requireLeaveId(leaveId);
        LeaveApplicationEntity leave = leaveApplicationRepository.findByLeaveIdForUpdate(leaveId)
                .orElseThrow(() -> notFound("LEAVE_NOT_FOUND", "Leave application not found."));
        if (!currentEmployeeId.equals(leave.getEmployeeId())) {
            throw new MobileApiException(
                    HttpStatus.FORBIDDEN,
                    "LEAVE_CANCELLATION_FORBIDDEN",
                    "You can cancel only your own leave application.");
        }
        if (!STATUS_PENDING.equalsIgnoreCase(trim(leave.getStatus()))) {
            throw new MobileApiException(
                    HttpStatus.CONFLICT,
                    "LEAVE_NOT_CANCELLABLE",
                    "Only pending leave applications can be cancelled.");
        }
        leave.setStatus("CANCELLED");
        leave.setManagerRemarks("Cancelled by employee before approval.");
        leaveApplicationRepository.save(leave);
        return new MobileLeaveApplicationResponse(
                true,
                "Leave application cancelled successfully.",
                toApplication(leave));
    }

    @Override
    @Transactional(readOnly = true)
    public MobileLeaveApprovalsResponse getApprovals(Long employeeId, String query) {
        MobileEmployeeAccessContext context = requireHodContext(employeeId);
        String normalizedQuery = textOrNull(query);
        List<MobileLeaveApprovalsResponse.ApprovalItem> pending = leaveApplicationService
                .getPendingLeavesForHOD(context.user().getId(), normalizedQuery)
                .stream()
                .map(this::toApprovalItem)
                .toList();
        List<MobileLeaveApprovalsResponse.ApprovalItem> processed = leaveApplicationService
                .getProcessedLeavesForHOD(context.user().getId(), normalizedQuery)
                .stream()
                .map(this::toApprovalItem)
                .toList();
        return new MobileLeaveApprovalsResponse(
                true,
                "Leave approvals fetched successfully.",
                context.employee().getEmployeeId(),
                normalizedQuery,
                pending,
                processed);
    }

    @Override
    @Transactional
    public MobileLeaveApplicationResponse approve(Long employeeId, Long leaveId, String remarks) {
        return decide(employeeId, leaveId, STATUS_APPROVED, remarks);
    }

    @Override
    @Transactional
    public MobileLeaveApplicationResponse reject(Long employeeId, Long leaveId, String remarks) {
        if (!StringUtils.hasText(remarks)) {
            throw badRequest("REJECTION_REMARKS_REQUIRED", "Remarks are required when rejecting leave.");
        }
        return decide(employeeId, leaveId, STATUS_REJECTED, remarks);
    }

    private MobileLeaveApplicationResponse decide(
            Long employeeId,
            Long leaveId,
            String decisionStatus,
            String remarks) {
        MobileEmployeeAccessContext context = requireHodContext(employeeId);
        requireLeaveId(leaveId);
        LeaveApplicationEntity leave = leaveApplicationRepository.findByLeaveIdForUpdate(leaveId)
                .orElseThrow(() -> notFound("LEAVE_NOT_FOUND", "Leave application not found."));

        Set<Long> authorizedEmployeeIds = Set.copyOf(
                reportingManagerService.getEffectiveEmployeeIdsForAuthority(context.user().getId()));
        if (!authorizedEmployeeIds.contains(leave.getEmployeeId())) {
            throw new MobileApiException(
                    HttpStatus.FORBIDDEN,
                    "LEAVE_APPROVAL_FORBIDDEN",
                    "This leave application is outside your reporting authority.");
        }
        if (!STATUS_PENDING.equalsIgnoreCase(trim(leave.getStatus()))) {
            throw new MobileApiException(
                    HttpStatus.CONFLICT,
                    "LEAVE_ALREADY_PROCESSED",
                    "Only pending leave applications can be processed.");
        }

        leave.setStatus(decisionStatus);
        leave.setHodRemarks(textOrNull(remarks));
        leaveApplicationRepository.save(leave);

        String action = STATUS_APPROVED.equals(decisionStatus) ? "approved" : "rejected";
        return new MobileLeaveApplicationResponse(
                true,
                "Leave application " + action + " successfully.",
                toApplication(leave));
    }

    private MobileEmployeeAccessContext requireHodContext(Long employeeId) {
        MobileEmployeeAccessContext context = mobileEmployeeAccessService.requireCurrentActiveEmployeeContext(employeeId);
        boolean hod = context.user().getRoles() != null
                && context.user().getRoles().stream()
                        .anyMatch(role -> role != null && ROLE_HOD.equalsIgnoreCase(trim(role.getName())));
        if (!hod) {
            throw new MobileApiException(
                    HttpStatus.FORBIDDEN,
                    "HOD_ACCESS_REQUIRED",
                    "HOD access is required for leave approvals.");
        }
        return context;
    }

    private List<MobileLeaveOptionsResponse.LeaveType> getLeaveTypes() {
        Map<String, MobileLeaveOptionsResponse.LeaveType> uniqueTypes = new LinkedHashMap<>();
        for (LeaveEntity leave : leaveRepository.findAll()) {
            if (leave == null) {
                continue;
            }
            boolean compOff = isCompOff(leave.getLeaveCode()) || isCompOff(leave.getLeaveName());
            String code = textOrNull(leave.getLeaveCode());
            if (compOff && code == null) {
                code = COMP_OFF_CODE;
            }
            if (code == null) {
                continue;
            }
            String name = StringUtils.hasText(leave.getLeaveName()) ? leave.getLeaveName().trim() : code;
            uniqueTypes.putIfAbsent(
                    normalize(code),
                    new MobileLeaveOptionsResponse.LeaveType(leave.getLeaveId(), code, name, compOff));
        }
        uniqueTypes.putIfAbsent(
                COMP_OFF_CODE,
                new MobileLeaveOptionsResponse.LeaveType(null, COMP_OFF_CODE, "Comp Off", true));

        List<MobileLeaveOptionsResponse.LeaveType> types = new ArrayList<>(uniqueTypes.values());
        types.sort(Comparator.comparing(
                MobileLeaveOptionsResponse.LeaveType::name,
                String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(types);
    }

    private String resolveLeaveType(String requestedType) {
        String normalizedRequest = normalize(requestedType);
        return getLeaveTypes().stream()
                .filter(type -> normalize(type.code()).equals(normalizedRequest)
                        || normalize(type.name()).equals(normalizedRequest))
                .map(MobileLeaveOptionsResponse.LeaveType::code)
                .findFirst()
                .orElseThrow(() -> badRequest(
                        "INVALID_LEAVE_TYPE",
                        "Select a valid leave type returned by the leave options API."));
    }

    private String resolveLeaveCategory(String requestedCategory) {
        String normalizedRequest = normalize(requestedCategory);
        return LEAVE_CATEGORIES.stream()
                .filter(category -> normalize(category.code()).equals(normalizedRequest)
                        || normalize(category.name()).equals(normalizedRequest))
                .map(MobileLeaveOptionsResponse.LeaveCategory::name)
                .findFirst()
                .orElseThrow(() -> badRequest(
                        "INVALID_LEAVE_CATEGORY",
                        "Leave category must be FULL_DAY, FIRST_HALF, or SECOND_HALF."));
    }

    private MobileLeaveApplication toApplication(LeaveApplicationEntity leave) {
        return new MobileLeaveApplication(
                leave.getLeaveId(),
                leave.getEmployeeId(),
                leave.getLeaveType(),
                leave.getLeaveCategory(),
                leave.getStartDate(),
                leave.getEndDate(),
                leave.getCompOffWorkDate(),
                leave.getDescription(),
                leave.getApplicationDate(),
                leave.getStatus(),
                leave.getHodRemarks(),
                leave.getManagerRemarks(),
                STATUS_PENDING.equalsIgnoreCase(trim(leave.getStatus())));
    }

    private MobileLeaveApprovalsResponse.ApprovalItem toApprovalItem(LeaveApplicationHODDTO leave) {
        return new MobileLeaveApprovalsResponse.ApprovalItem(
                leave.getLeaveId(),
                leave.getEmployeeId(),
                leave.getEmployeeCode(),
                leave.getEmployeeName(),
                leave.getDesignation(),
                leave.getLeaveType(),
                leave.getLeaveCategory(),
                leave.getStartDate(),
                leave.getEndDate(),
                leave.getCompOffWorkDate(),
                leave.getDescription(),
                leave.getApplicationDate(),
                leave.getStatus(),
                leave.getHodRemarks());
    }

    private void requireLeaveId(Long leaveId) {
        if (leaveId == null || leaveId <= 0) {
            throw badRequest("LEAVE_ID_REQUIRED", "A valid leave application ID is required.");
        }
    }

    private boolean isCompOff(String value) {
        String normalized = normalize(value);
        return normalized.equals("CO")
                || normalized.equals("COMPOFF")
                || normalized.equals("COMPOFFLEAVE")
                || normalized.equals("COMPENSATORYOFF")
                || normalized.equals("COMPENSATORYLEAVE");
    }

    private String normalize(String value) {
        String normalized = trim(value);
        return normalized == null
                ? ""
                : normalized.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private String textOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private MobileApiException badRequest(String code, String message) {
        return new MobileApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    private MobileApiException notFound(String code, String message) {
        return new MobileApiException(HttpStatus.NOT_FOUND, code, message);
    }
}
