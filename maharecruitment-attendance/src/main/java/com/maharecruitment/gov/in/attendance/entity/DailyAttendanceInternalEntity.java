package com.maharecruitment.gov.in.attendance.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "daily_attendance_internal_employee")
public class DailyAttendanceInternalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "in_time")
    private String inTime;

    @Column(name = "out_time")
    private String outTime;

    @Column(name = "total_hours")
    private String totalHours;

    @Column(name = "status")
    private String status;

    @Column(name = "month_val")
    private Integer month;

    @Column(name = "year_val")
    private Integer year;
}
