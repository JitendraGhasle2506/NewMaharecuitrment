package com.maharecruitment.gov.in.web.dto.agency;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgencyInterviewScheduleForm {

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime interviewDateTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate interviewDate;

    private String interviewTimeSlot;

    private String interviewLink;

    private String interviewRemarks;
}
