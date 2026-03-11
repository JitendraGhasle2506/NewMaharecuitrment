package com.maharecruitment.gov.in.department.service;

import java.util.List;

import com.maharecruitment.gov.in.department.dto.AdvancePaymentForm;
import com.maharecruitment.gov.in.department.dto.DepartmentProjectApplicationSummaryView;
import com.maharecruitment.gov.in.department.entity.AuditorReviewDecision;
import com.maharecruitment.gov.in.department.entity.DepartmentAdvancePaymentEntity;
import com.maharecruitment.gov.in.department.entity.HrReviewDecision;
import com.maharecruitment.gov.in.master.dto.ProformaInvoiceSummary;

public interface DepartmentAdvancePaymentService {

    AdvancePaymentForm initializePaymentForm(Long applicationId, String actorEmail);

    DepartmentProjectApplicationSummaryView getProjectApplicationSummary(Long applicationId);

    List<ProformaInvoiceSummary> getAvailableInvoices(Long applicationId);

    Long savePayment(AdvancePaymentForm form, String actionType, String actorEmail);

    AdvancePaymentForm getPaymentForEdit(Long paymentId, String actorEmail);

    List<DepartmentAdvancePaymentEntity> getPaymentSummaries(String actorEmail);

    List<DepartmentProjectApplicationSummaryView> getEligibleProjectsForAdvancePayment(String actorEmail);

    boolean isReceiptNumberDuplicate(String receiptNumber, Long paymentId);

    void reviewByHr(Long paymentId, HrReviewDecision decision, String remarks, String actorEmail);

    void reviewByAuditor(Long paymentId, AuditorReviewDecision decision, String remarks, String actorEmail);

    List<DepartmentAdvancePaymentEntity> getReviewList(String actorEmail);

    AdvancePaymentForm getPaymentForReview(Long paymentId, String actorEmail);

    org.springframework.core.io.Resource getReceiptResource(Long paymentId, String actorEmail);
}
