package com.maharecruitment.gov.in.recruitment.entity;

import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** PIP is independent of employment status: employees remain active while under review. */
@Entity
@Table(name = "employee_pip_plan")
@Getter
@Setter
public class EmployeePipPlan extends RecruitmentAuditable {
    public enum Status { UNDER_REVIEW, COMPLETED, EXIT_RECORDED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;
    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "duration_months", nullable = false)
    private int durationMonths;
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    @Column(nullable = false, length = 2000)
    private String objectives;
    @Column(length = 2000)
    private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Status status = Status.UNDER_REVIEW;
    @Column(name = "decision_by_user_id")
    private Long decisionByUserId;
    @Column(name = "decision_date")
    private LocalDate decisionDate;
    @Column(name = "decision_feedback", length = 2000)
    private String decisionFeedback;
    @Column(name = "exit_date")
    private LocalDate exitDate;
    @Version
    private long version;
}
