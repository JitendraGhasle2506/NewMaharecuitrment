package com.maharecruitment.gov.in.department.service;

import java.util.List;

import com.maharecruitment.gov.in.department.service.model.HrDepartmentSubDepartmentRequestView;
import com.maharecruitment.gov.in.department.service.model.HrParentDepartmentRequestView;
import com.maharecruitment.gov.in.department.service.model.HrSubDepartmentApplicationDetailView;

public interface HrDepartmentRequestService {

    List<HrParentDepartmentRequestView> getParentDepartmentRequests();

    HrDepartmentSubDepartmentRequestView getSubDepartmentProjectCounts(Long departmentId);

    HrSubDepartmentApplicationDetailView getSubDepartmentApplications(Long departmentId, Long subDepartmentId);
}
