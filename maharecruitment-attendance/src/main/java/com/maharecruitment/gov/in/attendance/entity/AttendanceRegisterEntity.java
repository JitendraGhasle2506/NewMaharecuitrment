package com.maharecruitment.gov.in.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
	@Entity
	@Table(name = "attendance_daily")
	public class AttendanceRegisterEntity {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@Column(name = "user_id", nullable = false)
		private Long userId;

		@Column(name = "month", nullable = false)
		private Integer month;

		@Column(name = "year", nullable = false)
		private Integer year;

		@Column(name = "d1")
		private String d1;
		@Column(name = "d2")
		private String d2;
		@Column(name = "d3")
		private String d3;
		@Column(name = "d4")
		private String d4;
		@Column(name = "d5")
		private String d5;
		@Column(name = "d6")
		private String d6;
		@Column(name = "d7")
		private String d7;
		@Column(name = "d8")
		private String d8;
		@Column(name = "d9")
		private String d9;
		@Column(name = "d10")
		private String d10;
		@Column(name = "d11")
		private String d11;
		@Column(name = "d12")
		private String d12;
		@Column(name = "d13")
		private String d13;
		@Column(name = "d14")
		private String d14;
		@Column(name = "d15")
		private String d15;
		@Column(name = "d16")
		private String d16;
		@Column(name = "d17")
		private String d17;
		@Column(name = "d18")
		private String d18;
		@Column(name = "d19")
		private String d19;
		@Column(name = "d20")
		private String d20;
		@Column(name = "d21")
		private String d21;
		@Column(name = "d22")
		private String d22;
		@Column(name = "d23")
		private String d23;
		@Column(name = "d24")
		private String d24;
		@Column(name = "d25")
		private String d25;
		@Column(name = "d26")
		private String d26;
		@Column(name = "d27")
		private String d27;
		@Column(name = "d28")
		private String d28;
		@Column(name = "d29")
		private String d29;
		@Column(name = "d30")
		private String d30;
		@Column(name = "d31")
		private String d31;

	    @Column(name = "request_id")
	    private String requestId;
	    
	    @Column(name = "department")
	    private String department;

	    @Column(name = "sub_dept_name")
	    private String subDeptName;

	    @Column(name = "designation")
	    private String designation;

	    @Column(name = "designation_id")
	    private Long designationId;

	    @Column(name = "level_code")
	    private String levelCode;
	    @Column(name = "name")
	    private String name;
	}