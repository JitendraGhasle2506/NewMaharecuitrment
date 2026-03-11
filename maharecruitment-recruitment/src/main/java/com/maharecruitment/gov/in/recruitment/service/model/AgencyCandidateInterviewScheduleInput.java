package com.maharecruitment.gov.in.recruitment.service.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgencyCandidateInterviewScheduleInput {

    private LocalDateTime interviewDateTime;

    private String interviewTimeSlot;

    private String interviewLink;

    private String interviewRemarks;
}
