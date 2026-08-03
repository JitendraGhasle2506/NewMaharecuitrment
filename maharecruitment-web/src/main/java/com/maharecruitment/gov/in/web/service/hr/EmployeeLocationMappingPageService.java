package com.maharecruitment.gov.in.web.service.hr;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.web.service.hr.model.EmployeeLocationMappingEditView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeLocationMappingEmployeeView;

public interface EmployeeLocationMappingPageService {

    Page<EmployeeLocationMappingEmployeeView> searchEmployees(
            String recruitmentType,
            String searchText,
            Pageable pageable);

    EmployeeLocationMappingEditView loadMapping(Long employeeId);

    boolean updateMapping(
            Long employeeId,
            List<Long> selectedLocationIds,
            Long primaryLocationId,
            String actorLoginId);
}
