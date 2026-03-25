package com.maharecruitment.gov.in.web.dto.agency;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgencyInterviewScheduleForm {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate interviewDate;

    private String interviewTimeSlot;

    private String interviewLink;

    private String interviewRemarks;
}
