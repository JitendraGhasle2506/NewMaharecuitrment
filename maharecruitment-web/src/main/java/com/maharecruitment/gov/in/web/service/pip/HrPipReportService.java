package com.maharecruitment.gov.in.web.service.pip;

import org.springframework.data.domain.Page;
import com.maharecruitment.gov.in.recruitment.entity.EmployeePipPlan.Status;
import com.maharecruitment.gov.in.web.service.pip.EmployeePipService.Detail;
import com.maharecruitment.gov.in.web.service.pip.EmployeePipService.PlanView;

/** HR oversight grants no PIP creation, feedback or outcome permissions. */
public interface HrPipReportService {
    record Overview(Page<PlanView> plans, long underReview, long completed, long exited) { }
    Overview overview(String login, Status status, String search, int page);
    Detail detail(String login, Long id);
}
