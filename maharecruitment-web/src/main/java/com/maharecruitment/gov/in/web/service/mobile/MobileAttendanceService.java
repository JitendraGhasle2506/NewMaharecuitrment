package com.maharecruitment.gov.in.web.service.mobile;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.web.dto.mobile.MobileAttendanceHistoryResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileAttendanceResponse;

public interface MobileAttendanceService {

    MobileAttendanceResponse checkIn(
            Long employeeId,
            BigDecimal latitude,
            BigDecimal longitude,
            String locationAddress,
            MultipartFile image);

    MobileAttendanceResponse checkOut(
            Long employeeId,
            BigDecimal latitude,
            BigDecimal longitude,
            String locationAddress,
            MultipartFile image);

    MobileAttendanceHistoryResponse getHistory(Long employeeId, LocalDate fromDate, LocalDate toDate);
}
