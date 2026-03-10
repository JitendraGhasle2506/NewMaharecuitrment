package com.maharecruitment.gov.in.department.service;

import java.util.List;

import com.maharecruitment.gov.in.department.entity.AuditorReviewDecision;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.service.model.DepartmentProfileDocumentType;
import com.maharecruitment.gov.in.department.service.model.AuditorDepartmentApplicationReviewDetailView;
import com.maharecruitment.gov.in.department.service.model.AuditorDepartmentSubDepartmentRequestView;
import com.maharecruitment.gov.in.department.service.model.AuditorParentDepartmentRequestView;
import com.maharecruitment.gov.in.department.service.model.AuditorSubDepartmentApplicationDetailView;
import com.maharecruitment.gov.in.department.service.model.WorkOrderDocumentView;

public interface AuditorDepartmentRequestService {

    List<AuditorParentDepartmentRequestView> getParentDepartmentRequests();

    AuditorDepartmentSubDepartmentRequestView getSubDepartmentProjectCounts(Long departmentId);

    AuditorSubDepartmentApplicationDetailView getSubDepartmentApplications(Long departmentId, Long subDepartmentId);

    AuditorDepartmentApplicationReviewDetailView getApplicationReviewDetail(
            Long departmentId,
            Long subDepartmentId,
            Long applicationId);

    DepartmentApplicationStatus reviewApplicationByAuditor(
            Long departmentId,
            Long subDepartmentId,
            Long applicationId,
            AuditorReviewDecision decision,
            String remarks,
            String actorEmail);

    DepartmentApplicationStatus completeApplication(
            Long departmentId,
            Long subDepartmentId,
            Long applicationId,
            String remarks,
            String actorEmail);

    WorkOrderDocumentView getApplicationWorkOrderDocument(
            Long departmentId,
            Long subDepartmentId,
            Long applicationId);

    WorkOrderDocumentView getDepartmentRegistrationDocument(
            Long departmentId,
            Long subDepartmentId,
            Long applicationId,
            DepartmentProfileDocumentType documentType);
}
