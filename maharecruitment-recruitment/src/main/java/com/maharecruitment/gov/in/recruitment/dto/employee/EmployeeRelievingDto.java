package com.maharecruitment.gov.in.recruitment.dto.employee;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRelievingDto {
    private Long relievingId;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String companyName;
    private String projectName;
    
    // Reason of relieving: Released, PIP, Return of Service
    private String reasonOfRelieving;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate exitDate;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate resignDate;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate pipStartDate;
    
    private String pipDuration;
    
    private Long handoverGivenToId;
    private String handoverGivenToName;
    private String status;
    private String remarks;
}
