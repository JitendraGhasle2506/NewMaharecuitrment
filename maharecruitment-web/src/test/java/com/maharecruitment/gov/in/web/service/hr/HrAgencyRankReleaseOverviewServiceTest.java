package com.maharecruitment.gov.in.web.service.hr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.department.service.HrDepartmentRequestService;
import com.maharecruitment.gov.in.department.service.impl.HrAgencyRankMappingServiceImpl;
import com.maharecruitment.gov.in.master.entity.AgencyMaster;
import com.maharecruitment.gov.in.master.repository.AgencyMasterRepository;
import com.maharecruitment.gov.in.master.repository.DepartmentMstRepository;
import com.maharecruitment.gov.in.master.repository.DesignationCategoryMasterRepository;
import com.maharecruitment.gov.in.master.repository.SubDepartmentRepository;
import com.maharecruitment.gov.in.recruitment.entity.AgencyGlobalRankEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus;
import com.maharecruitment.gov.in.recruitment.repository.AgencyGlobalRankRepository;
import com.maharecruitment.gov.in.recruitment.repository.AgencyNotificationTrackingRepository;
import com.maharecruitment.gov.in.recruitment.repository.RankReleaseRuleRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentNotificationRepository;
import com.maharecruitment.gov.in.recruitment.repository.projection.RecruitmentNotificationRankReleaseProjection;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentNotificationRankAssignmentService;

class HrAgencyRankReleaseOverviewServiceTest {

    @Test
    void overviewUsesProjectionAndToleratesMissingProject() {
        AgencyGlobalRankRepository globalRankRepository = mock(AgencyGlobalRankRepository.class);
        RecruitmentNotificationRepository notificationRepository = mock(RecruitmentNotificationRepository.class);
        AgencyNotificationTrackingRepository trackingRepository = mock(AgencyNotificationTrackingRepository.class);
        RankReleaseRuleRepository ruleRepository = mock(RankReleaseRuleRepository.class);

        AgencyMaster agency = new AgencyMaster();
        agency.setAgencyId(11L);
        agency.setAgencyName("Test Agency");

        AgencyGlobalRankEntity globalRank = new AgencyGlobalRankEntity();
        globalRank.setAgency(agency);
        globalRank.setRankNumber(1);
        globalRank.setAssignedDate(LocalDateTime.of(2026, 9, 1, 9, 0));

        RecruitmentNotificationRankReleaseProjection notification =
                mock(RecruitmentNotificationRankReleaseProjection.class);
        when(notification.getRecruitmentNotificationId()).thenReturn(27L);
        when(notification.getRequestId()).thenReturn("REQ-27");
        when(notification.getStatus()).thenReturn(RecruitmentNotificationStatus.IN_PROGRESS);
        when(notification.getCreatedDateTime()).thenReturn(LocalDateTime.of(2026, 9, 1, 10, 0));
        when(notification.getProjectName()).thenReturn(null);

        when(globalRankRepository.findAllWithAgencyOrderByRankNumberAscAgencyAgencyIdAsc())
                .thenReturn(List.of(globalRank));
        when(notificationRepository.findAllForRankReleaseOverview()).thenReturn(List.of(notification));
        when(ruleRepository.findAllByOrderByRankNumberAsc()).thenReturn(List.of());
        when(trackingRepository.findByRecruitmentNotificationIds(anyCollection())).thenReturn(List.of());

        HrAgencyRankMappingServiceImpl service = new HrAgencyRankMappingServiceImpl(
                mock(HrDepartmentRequestService.class),
                notificationRepository,
                globalRankRepository,
                trackingRepository,
                ruleRepository,
                mock(RecruitmentNotificationRankAssignmentService.class),
                mock(AgencyMasterRepository.class),
                mock(DepartmentProjectApplicationRepository.class),
                mock(DepartmentMstRepository.class),
                mock(SubDepartmentRepository.class),
                mock(DesignationCategoryMasterRepository.class));

        var view = service.getRankReleaseOverviewListView(0, 10);

        assertThat(view.getReleaseGroups()).hasSize(1);
        assertThat(view.getReleaseGroups().get(0).getProjectName()).isEqualTo("-");
        verify(notificationRepository).findAllForRankReleaseOverview();
        verify(notificationRepository, never()).findAll();
    }
}
