package com.maharecruitment.gov.in.web.service.dashboard.model;

import java.time.LocalDate;
import java.util.List;

public record HRAttendanceDetailView(
        LocalDate attendanceDate,
        HRAttendanceDetailCategory category,
        Long cellId,
        String cellName,
        Long designationId,
        String designationName,
        Long departmentId,
        String departmentName,
        List<HRAttendanceEmployeeView> employees,
        int pageNumber,
        int pageSize,
        boolean hasPrevious,
        boolean hasNext,
        HRAttendanceEmployeeType recruitmentType) {

    /** Unfiltered view (no recruitment-type restriction). */
    public HRAttendanceDetailView(
            LocalDate attendanceDate,
            HRAttendanceDetailCategory category,
            Long cellId,
            String cellName,
            Long designationId,
            String designationName,
            Long departmentId,
            String departmentName,
            List<HRAttendanceEmployeeView> employees,
            int pageNumber,
            int pageSize,
            boolean hasPrevious,
            boolean hasNext) {
        this(attendanceDate, category, cellId, cellName, designationId, designationName, departmentId,
                departmentName, employees, pageNumber, pageSize, hasPrevious, hasNext, null);
    }
}
