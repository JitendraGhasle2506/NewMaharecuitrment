package com.maharecruitment.gov.in.web.service.mobile.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockMultipartFile;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.attendance.entity.AttendanceSource;
import com.maharecruitment.gov.in.attendance.entity.DailyAttendanceInternalEntity;
import com.maharecruitment.gov.in.attendance.entity.HolidayMasterEntity;
import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;
import com.maharecruitment.gov.in.attendance.entity.TourApplicationEntity;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.attendance.repository.HolidayRepository;
import com.maharecruitment.gov.in.attendance.repository.LeaveApplicationRepository;
import com.maharecruitment.gov.in.attendance.repository.TourApplicationRepository;
import com.maharecruitment.gov.in.attendance.repository.WeekOffWorkingDayRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.dto.FileUploadResult;
import com.maharecruitment.gov.in.web.dto.mobile.MobileAttendanceHistoryResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileAttendanceResponse;
import com.maharecruitment.gov.in.web.service.mobile.MobileApiException;
import com.maharecruitment.gov.in.web.service.mobile.MobileEmployeeAccessService;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;

@ExtendWith(MockitoExtension.class)
class MobileAttendanceServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-06T10:15:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 6);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 6, 10, 15);

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DailyAttendanceInternalRepository dailyAttendanceInternalRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private WeekOffWorkingDayRepository weekOffWorkingDayRepository;

    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;

    @Mock
    private TourApplicationRepository tourApplicationRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void checkInCreatesMobileAttendanceForLoggedInEmployee() {
        authenticate("employee@example.com");
        EmployeeEntity employee = employee(101L, "EMP101", "employee@example.com");
        MockMultipartFile image = image();

        when(employeeRepository.findMobileLoginProfilesByEmail("employee@example.com"))
                .thenReturn(List.of(employee));
        when(dailyAttendanceInternalRepository.findByEmployeeIdAndAttendanceDate(101L, TODAY))
                .thenReturn(Optional.empty());
        when(fileStorageService.store(image, "mobile-attendance-photo"))
                .thenReturn(uploadResult("check-in.jpg", "/uploads/check-in.jpg"));
        when(dailyAttendanceInternalRepository.save(any(DailyAttendanceInternalEntity.class)))
                .thenAnswer(invocation -> {
                    DailyAttendanceInternalEntity attendance = invocation.getArgument(0);
                    attendance.setId(500L);
                    return attendance;
                });

        MobileAttendanceResponse response = service().checkIn(
                101L,
                new BigDecimal("19.0760000"),
                new BigDecimal("72.8777000"),
                "Mumbai Office",
                image);

        ArgumentCaptor<DailyAttendanceInternalEntity> captor =
                ArgumentCaptor.forClass(DailyAttendanceInternalEntity.class);
        verify(dailyAttendanceInternalRepository).save(captor.capture());
        DailyAttendanceInternalEntity saved = captor.getValue();
        assertThat(saved.getEmployeeId()).isEqualTo(101L);
        assertThat(saved.getEmployeeCode()).isEqualTo("EMP101");
        assertThat(saved.getAttendanceSource()).isEqualTo(AttendanceSource.MOBILE_APP);
        assertThat(saved.getAttendanceDate()).isEqualTo(TODAY);
        assertThat(saved.getCheckInTime()).isEqualTo(NOW);
        assertThat(saved.getInTime()).isEqualTo("10:15");
        assertThat(saved.getStatus()).isEqualTo("PRESENT");
        assertThat(saved.getCheckInLatitude()).isEqualByComparingTo("19.0760000");
        assertThat(saved.getCheckInLongitude()).isEqualByComparingTo("72.8777000");
        assertThat(saved.getCheckInLocationAddress()).isEqualTo("Mumbai Office");
        assertThat(saved.getCheckInImagePath()).isEqualTo("/uploads/check-in.jpg");
        assertThat(saved.getCreatedBy()).isEqualTo("employee@example.com");
        assertThat(saved.getUpdatedBy()).isEqualTo("employee@example.com");

        assertThat(response.success()).isTrue();
        assertThat(response.attendanceId()).isEqualTo(500L);
        assertThat(response.attendanceSource()).isEqualTo("MOBILE_APP");
    }

    @Test
    void checkInRejectsAnotherEmployeeId() {
        authenticate("employee@example.com");
        EmployeeEntity employee = employee(101L, "EMP101", "employee@example.com");
        when(employeeRepository.findMobileLoginProfilesByEmail("employee@example.com"))
                .thenReturn(List.of(employee));

        assertThatThrownBy(() -> service().checkIn(
                999L,
                new BigDecimal("19.0760000"),
                new BigDecimal("72.8777000"),
                "Mumbai Office",
                image()))
                .isInstanceOfSatisfying(MobileApiException.class, ex -> {
                    assertThat(ex.getStatus().value()).isEqualTo(403);
                    assertThat(ex.getCode()).isEqualTo("EMPLOYEE_MISMATCH");
                });

        verifyNoInteractions(fileStorageService);
        verify(dailyAttendanceInternalRepository, never()).save(any());
    }

    @Test
    void checkOutUpdatesExistingMobileAttendanceRow() {
        authenticate("employee@example.com");
        EmployeeEntity employee = employee(101L, "EMP101", "employee@example.com");
        MockMultipartFile image = image();

        DailyAttendanceInternalEntity existing = new DailyAttendanceInternalEntity();
        existing.setId(700L);
        existing.setEmployeeId(101L);
        existing.setEmployeeCode("EMP101");
        existing.setAttendanceDate(TODAY);
        existing.setAttendanceSource(AttendanceSource.MOBILE_APP);
        existing.setCheckInTime(LocalDateTime.of(2026, 7, 6, 9, 0));
        existing.setInTime("09:00");

        when(employeeRepository.findMobileLoginProfilesByEmail("employee@example.com"))
                .thenReturn(List.of(employee));
        when(dailyAttendanceInternalRepository.findByEmployeeIdAndAttendanceDate(101L, TODAY))
                .thenReturn(Optional.of(existing));
        when(fileStorageService.store(image, "mobile-attendance-photo"))
                .thenReturn(uploadResult("check-out.jpg", "/uploads/check-out.jpg"));
        when(dailyAttendanceInternalRepository.save(any(DailyAttendanceInternalEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MobileAttendanceResponse response = service().checkOut(
                101L,
                new BigDecimal("19.0760000"),
                new BigDecimal("72.8777000"),
                "Mumbai Office",
                image);

        verify(dailyAttendanceInternalRepository).save(existing);
        assertThat(existing.getCheckOutTime()).isEqualTo(NOW);
        assertThat(existing.getOutTime()).isEqualTo("10:15");
        assertThat(existing.getTotalHours()).isEqualTo("01:15");
        assertThat(existing.getCheckOutLatitude()).isEqualByComparingTo("19.0760000");
        assertThat(existing.getCheckOutLongitude()).isEqualByComparingTo("72.8777000");
        assertThat(existing.getCheckOutLocationAddress()).isEqualTo("Mumbai Office");
        assertThat(existing.getCheckOutImagePath()).isEqualTo("/uploads/check-out.jpg");

        assertThat(response.success()).isTrue();
        assertThat(response.attendanceId()).isEqualTo(700L);
        assertThat(response.checkOutTime()).isEqualTo(NOW);
    }

    @Test
    void historyReturnsAttendanceRowsWithCalculatedTotalHours() {
        authenticate("employee@example.com");
        EmployeeEntity employee = employee(101L, "EMP101", "employee@example.com");
        LocalDate fromDate = LocalDate.of(2026, 7, 5);
        LocalDate toDate = LocalDate.of(2026, 7, 6);
        DailyAttendanceInternalEntity olderAttendance = attendance(
                800L,
                fromDate,
                LocalDateTime.of(2026, 7, 5, 9, 0),
                LocalDateTime.of(2026, 7, 5, 18, 0),
                "09:00");
        DailyAttendanceInternalEntity latestAttendance = attendance(
                900L,
                toDate,
                LocalDateTime.of(2026, 7, 6, 9, 30),
                LocalDateTime.of(2026, 7, 6, 11, 15),
                null);

        when(employeeRepository.findMobileLoginProfilesByEmail("employee@example.com"))
                .thenReturn(List.of(employee));
        when(dailyAttendanceInternalRepository.findByEmployeeIdAndAttendanceDateBetween(101L, fromDate, toDate))
                .thenReturn(List.of(olderAttendance, latestAttendance));
        when(holidayRepository.findByHolidayDateBetween(fromDate, toDate)).thenReturn(List.of());
        when(weekOffWorkingDayRepository.findByWorkingDateBetween(fromDate, toDate)).thenReturn(List.of());
        when(leaveApplicationRepository
                .findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        101L,
                        "APPROVED",
                        toDate,
                        fromDate))
                .thenReturn(List.of());
        when(tourApplicationRepository
                .findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        101L,
                        "APPROVED",
                        toDate,
                        fromDate))
                .thenReturn(List.of());

        MobileAttendanceHistoryResponse response = service().getHistory(101L, fromDate, toDate);

        assertThat(response.success()).isTrue();
        assertThat(response.employeeId()).isEqualTo(101L);
        assertThat(response.fromDate()).isEqualTo(fromDate);
        assertThat(response.toDate()).isEqualTo(toDate);
        assertThat(response.attendanceHistory()).hasSize(2);

        MobileAttendanceHistoryResponse.AttendanceEntry latestRecord = response.attendanceHistory().getFirst();
        assertThat(latestRecord.attendanceId()).isEqualTo(900L);
        assertThat(latestRecord.attendanceDate()).isEqualTo(toDate);
        assertThat(latestRecord.checkInTime()).isEqualTo(LocalDateTime.of(2026, 7, 6, 9, 30));
        assertThat(latestRecord.checkOutTime()).isEqualTo(LocalDateTime.of(2026, 7, 6, 11, 15));
        assertThat(latestRecord.totalHours()).isEqualTo("01:45");
        assertThat(latestRecord.checkedIn()).isTrue();
        assertThat(latestRecord.checkedOut()).isTrue();
        assertThat(latestRecord.attendanceSource()).isEqualTo("MOBILE_APP");
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void historyGeneratesAbsentHolidayWeekOffLeaveAndTourRows() {
        authenticate("employee@example.com");
        EmployeeEntity employee = employee(101L, "EMP101", "employee@example.com");
        LocalDate fromDate = LocalDate.of(2026, 7, 1);
        LocalDate toDate = LocalDate.of(2026, 7, 6);
        DailyAttendanceInternalEntity presentAttendance = attendance(
                900L,
                toDate,
                LocalDateTime.of(2026, 7, 6, 9, 30),
                LocalDateTime.of(2026, 7, 6, 18, 0),
                null);

        when(employeeRepository.findMobileLoginProfilesByEmail("employee@example.com"))
                .thenReturn(List.of(employee));
        when(dailyAttendanceInternalRepository.findByEmployeeIdAndAttendanceDateBetween(101L, fromDate, toDate))
                .thenReturn(List.of(presentAttendance));
        when(holidayRepository.findByHolidayDateBetween(fromDate, toDate))
                .thenReturn(List.of(holiday(LocalDate.of(2026, 7, 3), "Public Holiday")));
        when(weekOffWorkingDayRepository.findByWorkingDateBetween(fromDate, toDate))
                .thenReturn(List.of());
        when(leaveApplicationRepository
                .findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        101L,
                        "APPROVED",
                        toDate,
                        fromDate))
                .thenReturn(List.of(leave(LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 2), "CL")));
        when(tourApplicationRepository
                .findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        101L,
                        "APPROVED",
                        toDate,
                        fromDate))
                .thenReturn(List.of(tour(LocalDate.of(2026, 7, 4), LocalDate.of(2026, 7, 4))));

        MobileAttendanceHistoryResponse response = service().getHistory(101L, fromDate, toDate);

        assertThat(response.attendanceHistory())
                .extracting(MobileAttendanceHistoryResponse.AttendanceEntry::attendanceDate)
                .containsExactly(
                        LocalDate.of(2026, 7, 6),
                        LocalDate.of(2026, 7, 5),
                        LocalDate.of(2026, 7, 4),
                        LocalDate.of(2026, 7, 3),
                        LocalDate.of(2026, 7, 2),
                        LocalDate.of(2026, 7, 1));
        assertThat(response.attendanceHistory())
                .extracting(MobileAttendanceHistoryResponse.AttendanceEntry::status)
                .containsExactly("PRESENT", "WEEK_OFF", "TOUR", "HOLIDAY", "LEAVE", "ABSENT");
        assertThat(response.attendanceHistory().get(1).attendanceId()).isNull();
        assertThat(response.attendanceHistory().get(1).checkedIn()).isFalse();
        assertThat(response.attendanceHistory().get(1).totalHours()).isNull();
    }

    private MobileAttendanceServiceImpl service() {
        return new MobileAttendanceServiceImpl(
                new MobileEmployeeAccessService(userRepository, employeeRepository),
                dailyAttendanceInternalRepository,
                fileStorageService,
                holidayRepository,
                weekOffWorkingDayRepository,
                leaveApplicationRepository,
                tourApplicationRepository,
                CLOCK);
    }

    private EmployeeEntity employee(Long employeeId, String employeeCode, String email) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeCode(employeeCode);
        employee.setEmail(email);
        employee.setFullName("Test Employee");
        employee.setMobile("9876543210");
        employee.setStatus("ACTIVE");
        return employee;
    }

    private DailyAttendanceInternalEntity attendance(
            Long attendanceId,
            LocalDate attendanceDate,
            LocalDateTime checkInTime,
            LocalDateTime checkOutTime,
            String totalHours) {
        DailyAttendanceInternalEntity attendance = new DailyAttendanceInternalEntity();
        attendance.setId(attendanceId);
        attendance.setEmployeeId(101L);
        attendance.setEmployeeCode("EMP101");
        attendance.setAttendanceDate(attendanceDate);
        attendance.setAttendanceSource(AttendanceSource.MOBILE_APP);
        attendance.setCheckInTime(checkInTime);
        attendance.setCheckOutTime(checkOutTime);
        attendance.setInTime(checkInTime != null ? checkInTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) : null);
        attendance.setOutTime(checkOutTime != null ? checkOutTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) : null);
        attendance.setTotalHours(totalHours);
        attendance.setStatus("PRESENT");
        return attendance;
    }

    private HolidayMasterEntity holiday(LocalDate holidayDate, String holidayName) {
        HolidayMasterEntity holiday = new HolidayMasterEntity();
        holiday.setHolidayDate(holidayDate);
        holiday.setHolidayName(holidayName);
        return holiday;
    }

    private LeaveApplicationEntity leave(LocalDate startDate, LocalDate endDate, String leaveType) {
        LeaveApplicationEntity leave = new LeaveApplicationEntity();
        leave.setEmployeeId(101L);
        leave.setStartDate(startDate);
        leave.setEndDate(endDate);
        leave.setLeaveType(leaveType);
        leave.setStatus("APPROVED");
        return leave;
    }

    private TourApplicationEntity tour(LocalDate startDate, LocalDate endDate) {
        TourApplicationEntity tour = new TourApplicationEntity();
        tour.setEmployeeId(101L);
        tour.setStartDate(startDate);
        tour.setEndDate(endDate);
        tour.setStatus("APPROVED");
        return tour;
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue(email)).thenReturn(Optional.of(user(email)));
    }

    private User user(String email) {
        User user = new User();
        user.setId(10L);
        user.setName("Test Employee");
        user.setEmail(email);
        user.setMobileNo("9876543210");
        user.setActive(true);
        return user;
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("image", "attendance.jpg", "image/jpeg", new byte[] { 1, 2, 3 });
    }

    private FileUploadResult uploadResult(String originalFileName, String fullPath) {
        return new FileUploadResult(originalFileName, originalFileName, fullPath, "image/jpeg", 3);
    }
}
