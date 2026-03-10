package com.maharecruitment.gov.in.department.service;

import java.math.BigDecimal;
import java.util.List;

import com.maharecruitment.gov.in.master.dto.ManpowerDesignationMasterResponse;
import com.maharecruitment.gov.in.department.entity.AuditorReviewDecision;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.entity.HrReviewDecision;
import com.maharecruitment.gov.in.department.dto.DepartmentProjectApplicationActivityView;
import com.maharecruitment.gov.in.department.dto.DepartmentProjectApplicationForm;
import com.maharecruitment.gov.in.department.dto.DepartmentProjectApplicationSummaryView;
import com.maharecruitment.gov.in.department.dto.LevelOptionView;
import com.maharecruitment.gov.in.department.service.model.WorkOrderDocumentView;

public interface DepartmentManpowerApplicationService {

    DepartmentProjectApplicationForm initializeApplicationForm(String actorEmail);

    DepartmentProjectApplicationForm getApplicationForEdit(Long applicationId, String actorEmail);

    Long saveApplication(DepartmentProjectApplicationForm form, String actionStatus, String actorEmail);

    List<DepartmentProjectApplicationSummaryView> getApplicationSummaries(String actorEmail);

    List<DepartmentProjectApplicationActivityView> getApplicationActivities(Long applicationId, String actorEmail);

    List<ManpowerDesignationMasterResponse> getAvailableDesignations();

    List<LevelOptionView> getLevelsByDesignation(Long designationId);

    BigDecimal getMonthlyRate(Long designationId, String levelCode);

    WorkOrderDocumentView getWorkOrderDocument(Long applicationId, String actorEmail);

    DepartmentApplicationStatus reviewByHr(
            Long applicationId,
            HrReviewDecision decision,
            String remarks,
            String actorEmail);

    DepartmentApplicationStatus reviewByAuditor(
            Long applicationId,
            AuditorReviewDecision decision,
            String remarks,
            String actorEmail);

    DepartmentApplicationStatus markCompleted(
            Long applicationId,
            String remarks,
            String actorEmail);
}
