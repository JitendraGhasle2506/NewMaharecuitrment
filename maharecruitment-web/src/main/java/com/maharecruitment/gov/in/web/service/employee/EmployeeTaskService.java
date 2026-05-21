package com.maharecruitment.gov.in.web.service.employee;

import java.util.List;

import com.maharecruitment.gov.in.recruitment.dto.employee.EmployeeTaskLogDto;
import com.maharecruitment.gov.in.recruitment.dto.employee.TaskSubmissionForm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeTaskService {
    
    void saveTasks(TaskSubmissionForm taskForm, String loginEmail);

    Page<EmployeeTaskLogDto> getRecentTasks(String loginEmail, Integer month, Integer year, Pageable pageable);

    String fetchInTime(String loginEmail, String dateString);
}
