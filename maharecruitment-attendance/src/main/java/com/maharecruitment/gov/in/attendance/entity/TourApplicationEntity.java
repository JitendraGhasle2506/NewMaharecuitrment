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
import lombok.Data;

@Data
@Entity
@Table(name = "tour_application")
public class TourApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tourId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "tour_category")
    private String tourCategory; // Full Day, Half Day

    @Column(name = "time_period")
    private String timePeriod; // Fore Noon, Afternoon

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "start_date")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "application_date")
    private LocalDateTime applicationDate;

    @Column(name = "status")
    private String status; // PENDING, APPROVED, REJECTED

    @Column(name = "hod_remarks")
    private String hodRemarks;
}
