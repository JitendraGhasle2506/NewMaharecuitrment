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
@Table(name = "employee_task_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeTaskLogEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @Column(name = "project_name", length = 200)
    private String projectName;

    @Column(name = "module_name", length = 200)
    private String moduleName;

    @Column(name = "task_description", length = 1000)
    private String taskDescription;

    @Column(name = "task_date", nullable = false)
    private LocalDate taskDate;

    @Column(name = "hours_spent", nullable = false)
    private Double hoursSpent;

    @Column(name = "in_time", length = 20)
    private String inTime;

    @Column(name = "start_time", length = 20)
    private String startTime;

    @Column(name = "end_time", length = 20)
    private String endTime;

    @Column(name = "status", length = 50)
    private String status = "PENDING_APPROVAL";

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approval_date")
    private LocalDate approvalDate;

    @Column(name = "manager_remarks", length = 1000)
    private String managerRemarks;
}
