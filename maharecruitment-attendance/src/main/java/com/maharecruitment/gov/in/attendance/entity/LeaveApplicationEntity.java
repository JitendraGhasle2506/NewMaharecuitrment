package com.maharecruitment.gov.in.attendance.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Entity
@Table(name = "leave_application")
public class LeaveApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long leaveId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @NotBlank(message = "Leave type is required")
    @Column(name = "leave_type")
    private String leaveType;

    @NotBlank(message = "Leave category is required")
    @Column(name = "leave_category")
    private String leaveCategory;

    @NotNull(message = "Start date is required")
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    @Column(name = "start_date")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    @Column(name = "end_date")
    private LocalDate endDate;

    @DateTimeFormat(pattern = "dd-MM-yyyy")
    @Column(name = "comp_off_work_date")
    private LocalDate compOffWorkDate;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "application_date")
    private LocalDateTime applicationDate;

    @Column(name = "status")
    private String status;

    @Column(name = "hod_remarks")
    private String hodRemarks;

    @Column(name = "manager_remarks")
    private String managerRemarks;
}
