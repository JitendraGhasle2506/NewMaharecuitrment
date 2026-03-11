package com.maharecruitment.gov.in.web.dto.agency;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgencyInterviewScheduleForm {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime interviewDateTime;

    private String interviewTimeSlot;

    private String interviewLink;

    private String interviewRemarks;
}
