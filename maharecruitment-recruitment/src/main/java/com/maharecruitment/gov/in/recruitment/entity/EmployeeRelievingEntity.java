package com.maharecruitment.gov.in.recruitment.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "employee_relieving")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRelievingEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "relieving_id")
    private Long relievingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @Column(name = "reason_of_relieving")
    private String reasonOfRelieving; // Released, PIP, Return of Service

    @Column(name = "exit_date")
    private LocalDate exitDate;

    @Column(name = "resign_date")
    private LocalDate resignDate;

    @Column(name = "handover_given_to_id")
    private Long handoverGivenToId;

    @Column(name = "pip_start_date")
    private LocalDate pipStartDate;

    @Column(name = "pip_duration", length = 20)
    private String pipDuration;

    @Column(name = "status")
    private String status; // e.g., INITIATED, APPROVED, COMPLETED

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "detailed_reason", length = 255)
    private String detailedReason;

    @Column(name = "requested_last_working_date")
    private LocalDate requestedLastWorkingDate;

    @Column(name = "notice_period_shortfall", length = 100)
    private String noticePeriodShortfall;

    @Column(name = "active_projects_handover", length = 1000)
    private String activeProjectsHandover;

    @Column(name = "personal_email", length = 255)
    private String personalEmail;

    @Column(name = "alternate_mobile", length = 20)
    private String alternateMobile;

    @Column(name = "exit_feedback", columnDefinition = "TEXT")
    private String exitFeedback;

    @Column(name = "forwarding_address", columnDefinition = "TEXT")
    private String forwardingAddress;
}
