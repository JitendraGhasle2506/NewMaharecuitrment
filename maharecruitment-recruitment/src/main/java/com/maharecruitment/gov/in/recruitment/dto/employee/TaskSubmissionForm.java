package com.maharecruitment.gov.in.recruitment.dto.employee;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskSubmissionForm {
    private List<EmployeeTaskLogDto> taskList = new ArrayList<>();
}
