package com.maharecruitment.gov.in.web.dto.employee;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeProfileDTO {

    private Long id;

    private Long employeeId;

    private String fullName;

    private LocalDate dob;

    private String gender;

    private String alternateMobileNo;

    private String email;

    private String panNo;

    private String maritalStatus;

    private String bloodGroup;

    private String emergencyContactName;

    private String emergencyContactNo;

    private String currentAddress;

    private String permanentAddress;

    private String employeeCode;

    private String role;

    private String department;

    private String mobileNo;

    private String photoUrl;

    private boolean profileAvailable;

    private int completionPercentage;
}
