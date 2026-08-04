package com.maharecruitment.gov.in.web.service.hr;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.web.service.hr.model.EmployeeCellMappingEditView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeCellMappingEmployeeView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeCellBulkMappingResult;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeCellOptionView;

import java.util.List;

public interface EmployeeCellMappingPageService {

    Page<EmployeeCellMappingEmployeeView> searchEmployees(
            String recruitmentType,
            String searchText,
            Pageable pageable);

    EmployeeCellMappingEditView loadMapping(Long employeeId);

    List<EmployeeCellOptionView> availableActiveCells();

    boolean updateMapping(Long employeeId, Long cellId, String actorLoginId);

    EmployeeCellBulkMappingResult updateMappings(
            Long cellId,
            List<Long> employeeIds,
            String actorLoginId);
}
