package com.maharecruitment.gov.in.department.controller;

import java.security.Principal;
import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.maharecruitment.gov.in.department.dto.DepartmentReviewActionRequest;
import com.maharecruitment.gov.in.department.entity.AuditorReviewDecision;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.entity.HrReviewDecision;
import com.maharecruitment.gov.in.department.exception.DepartmentApplicationException;
import com.maharecruitment.gov.in.department.service.DepartmentManpowerApplicationService;
import com.maharecruitment.gov.in.master.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
public class DepartmentManpowerReviewController {

    private final DepartmentManpowerApplicationService manpowerApplicationService;

    public DepartmentManpowerReviewController(DepartmentManpowerApplicationService manpowerApplicationService) {
        this.manpowerApplicationService = manpowerApplicationService;
    }

    @PostMapping("/hr/department/manpower/{applicationId}/review")
    public ResponseEntity<ApiResponse<DepartmentApplicationStatus>> reviewByHr(
            @PathVariable Long applicationId,
            @Valid @RequestBody DepartmentReviewActionRequest request,
            Principal principal) {
        String actorEmail = resolveActorEmail(principal);
        HrReviewDecision decision = parseHrDecision(request.getDecision());
        DepartmentApplicationStatus status = manpowerApplicationService.reviewByHr(
                applicationId,
                decision,
                request.getRemarks(),
                actorEmail);
        return ResponseEntity.ok(ApiResponse.of("HR review decision applied successfully.", status));
    }

    @PostMapping("/auditor/department/manpower/{applicationId}/review")
    public ResponseEntity<ApiResponse<DepartmentApplicationStatus>> reviewByAuditor(
            @PathVariable Long applicationId,
            @Valid @RequestBody DepartmentReviewActionRequest request,
            Principal principal) {
        String actorEmail = resolveActorEmail(principal);
        AuditorReviewDecision decision = parseAuditorDecision(request.getDecision());
        DepartmentApplicationStatus status = manpowerApplicationService.reviewByAuditor(
                applicationId,
                decision,
                request.getRemarks(),
                actorEmail);
        return ResponseEntity.ok(ApiResponse.of("Auditor review decision applied successfully.", status));
    }

    @PostMapping("/auditor/department/manpower/{applicationId}/complete")
    public ResponseEntity<ApiResponse<DepartmentApplicationStatus>> markCompleted(
            @PathVariable Long applicationId,
            @Valid @RequestBody DepartmentReviewActionRequest request,
            Principal principal) {
        String actorEmail = resolveActorEmail(principal);
        DepartmentApplicationStatus status = manpowerApplicationService.markCompleted(
                applicationId,
                request.getRemarks(),
                actorEmail);
        return ResponseEntity.ok(ApiResponse.of("Application marked completed.", status));
    }

    private String resolveActorEmail(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new DepartmentApplicationException("Authenticated user is required.");
        }
        return principal.getName();
    }

    private HrReviewDecision parseHrDecision(String value) {
        try {
            return HrReviewDecision.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new DepartmentApplicationException("Invalid HR decision. Allowed: APPROVE, REJECT, SEND_BACK.");
        }
    }

    private AuditorReviewDecision parseAuditorDecision(String value) {
        try {
            return AuditorReviewDecision.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new DepartmentApplicationException("Invalid Auditor decision. Allowed: APPROVE, SEND_BACK.");
        }
    }
}
