package com.maharecruitment.gov.in.department.service;

import java.util.List;

import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.entity.HrReviewDecision;
import com.maharecruitment.gov.in.department.service.model.HrDepartmentSubDepartmentRequestView;
import com.maharecruitment.gov.in.department.service.model.HrDepartmentApplicationReviewDetailView;
import com.maharecruitment.gov.in.department.service.model.HrParentDepartmentRequestView;
import com.maharecruitment.gov.in.department.service.model.HrSubDepartmentApplicationDetailView;
import com.maharecruitment.gov.in.department.service.model.WorkOrderDocumentView;

public interface HrDepartmentRequestService {

    List<HrParentDepartmentRequestView> getParentDepartmentRequests();

    HrDepartmentSubDepartmentRequestView getSubDepartmentProjectCounts(Long departmentId);

    HrSubDepartmentApplicationDetailView getSubDepartmentApplications(Long departmentId, Long subDepartmentId);

    HrDepartmentApplicationReviewDetailView getApplicationReviewDetail(
            Long departmentId,
            Long subDepartmentId,
            Long applicationId);

    DepartmentApplicationStatus reviewApplicationByHr(
            Long departmentId,
            Long subDepartmentId,
            Long applicationId,
            HrReviewDecision decision,
            String remarks,
            String actorEmail);

    WorkOrderDocumentView getApplicationWorkOrderDocument(
            Long departmentId,
            Long subDepartmentId,
            Long applicationId);
}
