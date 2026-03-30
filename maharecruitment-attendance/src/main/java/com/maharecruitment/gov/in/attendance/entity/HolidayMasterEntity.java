package com.maharecruitment.gov.in.attendance.entity;


import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "holiday_master")
public class HolidayMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate holidayDate;

    private String holidayName;
}