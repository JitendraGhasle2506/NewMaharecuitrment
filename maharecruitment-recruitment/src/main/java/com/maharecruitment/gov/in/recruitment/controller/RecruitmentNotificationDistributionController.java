package com.maharecruitment.gov.in.recruitment.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maharecruitment.gov.in.recruitment.dto.AgencyVisibleNotificationResponse;
import com.maharecruitment.gov.in.recruitment.dto.AssignRecruitmentNotificationRanksRequest;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentAgencyNotificationActionService;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentAgencyNotificationQueryService;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentNotificationRankAssignmentService;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentNotificationRankReleaseService;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyRankAssignmentCommand;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyVisibleNotificationView;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/recruitment/notifications")
public class RecruitmentNotificationDistributionController {

    private final RecruitmentNotificationRankAssignmentService rankAssignmentService;
    private final RecruitmentNotificationRankReleaseService rankReleaseService;
    private final RecruitmentAgencyNotificationQueryService queryService;
    private final RecruitmentAgencyNotificationActionService actionService;

    public RecruitmentNotificationDistributionController(
            RecruitmentNotificationRankAssignmentService rankAssignmentService,
            RecruitmentNotificationRankReleaseService rankReleaseService,
            RecruitmentAgencyNotificationQueryService queryService,
            RecruitmentAgencyNotificationActionService actionService) {
        this.rankAssignmentService = rankAssignmentService;
        this.rankReleaseService = rankReleaseService;
        this.queryService = queryService;
        this.actionService = actionService;
    }

    @PostMapping("/{recruitmentNotificationId}/agency-ranks")
    public ResponseEntity<Void> assignAgencyRanks(
            @PathVariable Long recruitmentNotificationId,
            @Valid @RequestBody AssignRecruitmentNotificationRanksRequest request) {
        List<AgencyRankAssignmentCommand> assignments = request.getAgencyRanks()
                .stream()
                .map(item -> AgencyRankAssignmentCommand.builder()
                        .agencyId(item.getAgencyId())
                        .rankNumber(item.getRankNumber())
                        .build())
                .toList();

        rankAssignmentService.assignAgencyRanks(recruitmentNotificationId, assignments);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{recruitmentNotificationId}/release")
    public ResponseEntity<Integer> releaseEligibleRanks(
            @PathVariable Long recruitmentNotificationId) {
        int releasedCount = rankReleaseService.releaseEligibleRanksForNotification(recruitmentNotificationId);
        return ResponseEntity.ok(releasedCount);
    }

    @GetMapping("/agencies/{agencyId}/visible")
    public ResponseEntity<List<AgencyVisibleNotificationResponse>> getVisibleNotifications(
            @PathVariable Long agencyId) {
        List<AgencyVisibleNotificationResponse> responses = queryService
                .getVisibleNotifications(agencyId, null, Pageable.unpaged())
                .getContent()
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{recruitmentNotificationId}/agencies/{agencyId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long recruitmentNotificationId,
            @PathVariable Long agencyId) {
        actionService.markAsRead(recruitmentNotificationId, agencyId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{recruitmentNotificationId}/agencies/{agencyId}/respond")
    public ResponseEntity<Void> submitResponse(
            @PathVariable Long recruitmentNotificationId,
            @PathVariable Long agencyId) {
        actionService.submitResponse(recruitmentNotificationId, agencyId);
        return ResponseEntity.ok().build();
    }

    private AgencyVisibleNotificationResponse toResponse(AgencyVisibleNotificationView view) {
        return AgencyVisibleNotificationResponse.builder()
                .recruitmentNotificationId(view.getRecruitmentNotificationId())
                .requestId(view.getRequestId())
                .departmentRegistrationId(view.getDepartmentRegistrationId())
                .departmentProjectApplicationId(view.getDepartmentProjectApplicationId())
                .projectId(view.getProjectId())
                .projectName(view.getProjectName())
                .releasedRank(view.getReleasedRank())
                .notifiedAt(view.getNotifiedAt())
                .trackingStatus(view.getTrackingStatus())
                .notificationStatus(view.getNotificationStatus())
                .build();
    }
}
