package com.maharecruitment.gov.in.web.service.hr;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.web.service.hr.model.EmployeeProjectMappingEditView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeProjectMappingEmployeeView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeProjectBulkMappingResult;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeProjectOptionView;

public interface EmployeeProjectMappingPageService {

    Page<EmployeeProjectMappingEmployeeView> searchUnmappedEmployees(
            String recruitmentType,
            String searchText,
            Pageable pageable);

    Page<EmployeeProjectMappingEmployeeView> searchMappedEmployees(
            String recruitmentType,
            String searchText,
            Pageable pageable);

    EmployeeProjectMappingEditView loadMapping(Long employeeId);

    List<EmployeeProjectOptionView> availableActiveProjects();

    boolean updateMapping(Long employeeId, Long projectId);

    EmployeeProjectBulkMappingResult updateMappings(Long projectId, List<Long> employeeIds);
}
