package com.maharecruitment.gov.in.attendance.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ManualAttendanceRequestDTO {
    private Long id;
    private Long userId;
    private String employeeName;
    private LocalDate attendanceDate;
    private String inTime;
    private String outTime;
    private String reason;
    private String projectName;
    private String managerName;
    private String managerStatus;
    private String hodStatus;
    private String managerComments;
    private String hodComments;
    private LocalDateTime createdAt;
}
