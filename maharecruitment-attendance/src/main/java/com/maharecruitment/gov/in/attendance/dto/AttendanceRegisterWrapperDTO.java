package com.maharecruitment.gov.in.attendance.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRegisterWrapperDTO {

    private List<AttendanceRegisterDTO> dtos;
}
