package com.maharecruitment.gov.in.recruitment.service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgencyInternalAssessmentDetailView {

    private Long recruitmentNotificationId;
    private String requestId;
    private String projectName;
    private Long recruitmentInterviewDetailId;
    private String candidateName;
    private String candidateEmail;
    private String candidateMobile;
    private String candidateEducation;
    private String designationName;
    private String levelCode;
    private BigDecimal totalExperience;
    private BigDecimal relevantExperience;
    private String joiningTime;
    private String resumeFilePath;
    private LocalDateTime initialInterviewDateTime;
    private String initialInterviewTimeSlot;
    private String initialInterviewLink;
    private DepartmentInterviewAssessmentView assessment;
    private LocalDateTime levelTwoInterviewDateTime;
    private String levelTwoInterviewTimeSlot;
    private String levelTwoMeetingLink;
    private String levelTwoRemarks;
    private LocalDateTime levelTwoScheduledAt;
    private boolean levelTwoScheduled;
    private boolean levelTwoSchedulingAllowed;
    private boolean levelTwoTimeChangeRequested;
    private String levelTwoTimeChangeReason;
    private LocalDateTime levelTwoTimeChangeRequestedAt;
}
