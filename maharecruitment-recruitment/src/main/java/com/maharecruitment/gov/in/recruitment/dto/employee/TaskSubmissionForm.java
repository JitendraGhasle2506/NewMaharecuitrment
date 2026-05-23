package com.maharecruitment.gov.in.recruitment.dto.employee;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TaskSubmissionForm {
    private String entryMode = "DAILY";
    private LocalDate globalTaskDate;
    private List<EmployeeTaskLogDto> taskList = new ArrayList<>();
}
