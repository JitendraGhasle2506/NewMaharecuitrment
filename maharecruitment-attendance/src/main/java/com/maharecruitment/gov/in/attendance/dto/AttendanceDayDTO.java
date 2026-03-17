package com.maharecruitment.gov.in.attendance.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Data
public class AttendanceDayDTO {
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate date;

	private String inTime;
	private String outTime;
	private String status;
	private Double inHour;
	private Double outHour;
	private String locationIn;
	private String locationOut;
	private String stayHours;
	private boolean locked;

}