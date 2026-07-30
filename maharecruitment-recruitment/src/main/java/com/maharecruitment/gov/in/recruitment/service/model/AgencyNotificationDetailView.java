package com.maharecruitment.gov.in.recruitment.service.model;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgencyNotificationDetailView {

    private Long recruitmentNotificationId;

    private String requestId;

    private Long departmentRegistrationId;

    private Long departmentProjectApplicationId;

    private Long projectId;

    private String projectName;

    private boolean internalNotification;

    private RecruitmentNotificationStatus notificationStatus;

    // Tracking info for the current agency
    private Integer releasedRank;

    private LocalDateTime notifiedAt;

    private AgencyNotificationTrackingStatus trackingStatus;

    // Designation vacancies
    private List<DesignationVacancyView> designationVacancies;

    @Getter
    @Builder
    public static class DesignationVacancyView {
        private Long vacancyId;
        private String designationName;
        private String levelCode;
        private Long numberOfVacancy;
        private Long filledPost;
        private BigDecimal minExperience;
        private BigDecimal maxExperience;
        private String jobDescription;
    }
}
