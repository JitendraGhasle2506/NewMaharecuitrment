package com.maharecruitment.gov.in.web.service.dashboard.model;

import java.time.LocalDate;
import java.util.List;

public record HRAttendanceDetailView(
        LocalDate attendanceDate,
        HRAttendanceDetailCategory category,
        Long cellId,
        String cellName,
        List<HRAttendanceEmployeeView> employees,
        int pageNumber,
        int pageSize,
        boolean hasPrevious,
        boolean hasNext) {
}
