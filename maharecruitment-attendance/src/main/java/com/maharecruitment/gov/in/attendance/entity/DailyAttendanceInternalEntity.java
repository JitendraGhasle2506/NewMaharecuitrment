package com.maharecruitment.gov.in.attendance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "daily_attendance_internal_employee")
public class DailyAttendanceInternalEntity extends AttendanceAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "employee_code", length = 50)
    private String employeeCode;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_source", length = 30)
    private AttendanceSource attendanceSource = AttendanceSource.API;

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

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Column(name = "check_in_latitude", precision = 10, scale = 7)
    private BigDecimal checkInLatitude;

    @Column(name = "check_in_longitude", precision = 10, scale = 7)
    private BigDecimal checkInLongitude;

    @Column(name = "check_out_latitude", precision = 10, scale = 7)
    private BigDecimal checkOutLatitude;

    @Column(name = "check_out_longitude", precision = 10, scale = 7)
    private BigDecimal checkOutLongitude;

    @Column(name = "check_in_location_address", length = 1000)
    private String checkInLocationAddress;

    @Column(name = "check_out_location_address", length = 1000)
    private String checkOutLocationAddress;

    @Column(name = "check_in_image_path", length = 1000)
    private String checkInImagePath;

    @Column(name = "check_out_image_path", length = 1000)
    private String checkOutImagePath;
}
