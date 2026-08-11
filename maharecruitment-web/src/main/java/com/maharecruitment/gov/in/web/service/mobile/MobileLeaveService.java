package com.maharecruitment.gov.in.web.service.mobile;

import java.time.LocalDate;

import com.maharecruitment.gov.in.web.dto.mobile.MobileCompOffValidationResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLeaveApplicationResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLeaveApplyRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLeaveApprovalsResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLeaveHistoryResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLeaveOptionsResponse;

public interface MobileLeaveService {

    MobileLeaveOptionsResponse getOptions(Long employeeId);

    MobileLeaveApplicationResponse apply(MobileLeaveApplyRequest request);

    MobileLeaveHistoryResponse getApplications(Long employeeId);

    MobileCompOffValidationResponse validateCompOffWorkedDate(Long employeeId, LocalDate workedDate);

    MobileLeaveApplicationResponse cancel(Long employeeId, Long leaveId);

    MobileLeaveApprovalsResponse getApprovals(Long employeeId, String query);

    MobileLeaveApplicationResponse approve(Long employeeId, Long leaveId, String remarks);

    MobileLeaveApplicationResponse reject(Long employeeId, Long leaveId, String remarks);
}
