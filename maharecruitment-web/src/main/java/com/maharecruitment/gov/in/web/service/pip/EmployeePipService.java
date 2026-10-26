package com.maharecruitment.gov.in.web.service.pip;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import com.maharecruitment.gov.in.recruitment.entity.EmployeePipPlan.Status;

public interface EmployeePipService {
    String AUTHORIZATION = "hasAnyAuthority('ROLE_COO','ROLE_HOD','ROLE_MD','ROLE_CTO','ROLE_CFO')";
    enum Decision { CONTINUE, EXIT }
    record EmployeeOption(Long id, String name, String code) { }
    record PlanView(Long id, Long employeeId, String name, String code, LocalDate startDate,
                    LocalDate endDate, int durationMonths, Status status) { }
    record ReviewView(String reviewer, LocalDate date, int rating, String feedback) { }
    record Board(List<EmployeeOption> employees, Page<PlanView> plans) { }
    record Detail(PlanView plan, String objectives, List<ReviewView> reviews,
                  String decisionFeedback, LocalDate exitDate, boolean employeeActive, String reason) { }

    Board board(String login, String search, int page);
    Detail detail(String login, Long planId);
    Long start(String login, Long employeeId, LocalDate startDate, int months, String objectives, String reason);
    void review(String login, Long planId, LocalDate reviewDate, int rating, String feedback);
    void decide(String login, Long planId, Decision decision, String feedback, LocalDate exitDate);
}
