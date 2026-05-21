package com.maharecruitment.gov.in.web.service.employee;

import java.util.List;

import com.maharecruitment.gov.in.recruitment.dto.employee.ManagerTaskApprovalDto;

public interface ManagerTaskService {
    
    List<ManagerTaskApprovalDto> getPendingTasksForManager(String loginEmail);

    void approveTask(Long taskId, String loginEmail);

    void rejectTask(Long taskId, String remarks, String loginEmail);
}
