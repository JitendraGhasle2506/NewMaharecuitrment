package com.maharecruitment.gov.in.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.maharecruitment.gov.in.attendance.dto.AttendanceRegisterDTO;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.attendance.repository.HolidayRepository;
import com.maharecruitment.gov.in.attendance.repository.LeaveApplicationRepository;
import com.maharecruitment.gov.in.attendance.repository.ManualAttendanceRequestRepository;
import com.maharecruitment.gov.in.attendance.repository.TourApplicationRepository;
import com.maharecruitment.gov.in.attendance.repository.WeekOffWorkingDayRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

class AttendanceRegisterServiceImplSecurityTest {

    @Test
    void attendanceDtoContainsOnlyMaskedAadhaarAndNoPhotoFilePath() {
        AttendanceRegisterServiceImpl service = new AttendanceRegisterServiceImpl();
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        DailyAttendanceInternalRepository dailyRepository = mock(DailyAttendanceInternalRepository.class);
        HolidayRepository holidayRepository = mock(HolidayRepository.class);
        WeekOffWorkingDayRepository weekOffRepository = mock(WeekOffWorkingDayRepository.class);
        EmployeeReportingMappingRepository reportingRepository = mock(EmployeeReportingMappingRepository.class);
        LeaveApplicationRepository leaveRepository = mock(LeaveApplicationRepository.class);
        TourApplicationRepository tourRepository = mock(TourApplicationRepository.class);
        ManualAttendanceRequestRepository manualRepository = mock(ManualAttendanceRequestRepository.class);

        ReflectionTestUtils.setField(service, "employeeRepository", employeeRepository);
        ReflectionTestUtils.setField(service, "dailyAttendanceInternalRepository", dailyRepository);
        ReflectionTestUtils.setField(service, "holidayRepository", holidayRepository);
        ReflectionTestUtils.setField(service, "weekOffWorkingDayRepository", weekOffRepository);
        ReflectionTestUtils.setField(service, "employeeReportingMappingRepository", reportingRepository);
        ReflectionTestUtils.setField(service, "leaveApplicationRepository", leaveRepository);
        ReflectionTestUtils.setField(service, "tourApplicationRepository", tourRepository);
        ReflectionTestUtils.setField(service, "manualAttendanceRequestRepository", manualRepository);

        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(501L);
        employee.setFullName("Internal Employee");
        employee.setAadhaarNumber("1234 5678 9012");

        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year = today.getYear();
        LocalDate startDate = today.withDayOfMonth(1);
        LocalDate endDate = today.withDayOfMonth(today.lengthOfMonth());

        when(employeeRepository.findById(501L)).thenReturn(Optional.of(employee));
        when(dailyRepository.findByEmployeeIdAndMonthAndYear(501L, month, year)).thenReturn(List.of());
        when(dailyRepository.findByEmployeeIdAndAttendanceDate(501L, today)).thenReturn(Optional.empty());
        when(holidayRepository.findByHolidayDateBetween(startDate, endDate)).thenReturn(List.of());
        when(weekOffRepository.findByWorkingDateBetween(startDate, endDate)).thenReturn(List.of());
        when(reportingRepository.findByEmployeeId(501L)).thenReturn(null);
        when(leaveRepository.findByEmployeeIdAndStatus(501L, "APPROVED")).thenReturn(List.of());
        when(tourRepository.findByEmployeeIdAndStatus(501L, "APPROVED")).thenReturn(List.of());
        when(manualRepository.findByUserIdAndAttendanceDateBetween(501L, startDate, endDate))
                .thenReturn(List.of());

        AttendanceRegisterDTO dto = service.getInternalAttendanceForEmployee(501L, month, year);

        assertThat(dto.getAadhaarNumber()).isEqualTo("XXXXXXXX9012");
        assertThat(dto.toString()).doesNotContain("123456789012");
        assertThat(Arrays.stream(AttendanceRegisterDTO.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName))
                .doesNotContain("photoPath", "unmaskedAadhaar", "fullAadhaar");
    }
}
