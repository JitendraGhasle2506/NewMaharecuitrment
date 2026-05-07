package com.maharecruitment.gov.in.web.dto.admin;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminWeekOffWorkingDayView {

    private final Long id;
    private final LocalDate workingDate;
    private final String officeOrderOriginalName;
    private final String documentPathToken;
}
