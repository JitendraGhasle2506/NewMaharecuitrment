package com.maharecruitment.gov.in.web.service.mobile.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.attendance.entity.DailyAttendanceInternalEntity;
import com.maharecruitment.gov.in.attendance.entity.HolidayMasterEntity;
import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;
import com.maharecruitment.gov.in.attendance.entity.TourApplicationEntity;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.attendance.repository.HolidayRepository;
import com.maharecruitment.gov.in.attendance.repository.LeaveApplicationRepository;
import com.maharecruitment.gov.in.attendance.repository.TourApplicationRepository;
import com.maharecruitment.gov.in.attendance.repository.WeekOffWorkingDayRepository;
import com.maharecruitment.gov.in.attendance.service.AttendanceStatusResolver;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.web.dto.FileUploadResult;
import com.maharecruitment.gov.in.web.dto.mobile.MobileAttendanceHistoryResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileAttendanceResponse;
import com.maharecruitment.gov.in.web.service.mobile.MobileAttendanceException;
import com.maharecruitment.gov.in.web.service.mobile.MobileAttendanceService;
import com.maharecruitment.gov.in.web.service.mobile.MobileEmployeeAccessService;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;

@Service
public class MobileAttendanceServiceImpl implements MobileAttendanceService {

    private static final Logger log = LoggerFactory.getLogger(MobileAttendanceServiceImpl.class);
    private static final BigDecimal MIN_LATITUDE = BigDecimal.valueOf(-90);
    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final BigDecimal MIN_LONGITUDE = BigDecimal.valueOf(-180);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);
    private static final int COORDINATE_SCALE = 7;
    private static final int LOCATION_ADDRESS_MAX_LENGTH = 1000;
    private static final String PRESENT_STATUS = "PRESENT";
    private static final String APPROVED_STATUS = "APPROVED";
    private static final String MOBILE_ATTENDANCE_PHOTO_MODULE = "mobile-attendance-photo";

    private final MobileEmployeeAccessService mobileEmployeeAccessService;
    private final DailyAttendanceInternalRepository dailyAttendanceInternalRepository;
    private final FileStorageService fileStorageService;
    private final HolidayRepository holidayRepository;
    private final WeekOffWorkingDayRepository weekOffWorkingDayRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final TourApplicationRepository tourApplicationRepository;
    private final Clock clock;

    @Autowired
    public MobileAttendanceServiceImpl(
            MobileEmployeeAccessService mobileEmployeeAccessService,
            DailyAttendanceInternalRepository dailyAttendanceInternalRepository,
            FileStorageService fileStorageService,
            HolidayRepository holidayRepository,
            WeekOffWorkingDayRepository weekOffWorkingDayRepository,
            LeaveApplicationRepository leaveApplicationRepository,
            TourApplicationRepository tourApplicationRepository) {
        this(
                mobileEmployeeAccessService,
                dailyAttendanceInternalRepository,
                fileStorageService,
                holidayRepository,
                weekOffWorkingDayRepository,
                leaveApplicationRepository,
                tourApplicationRepository,
                Clock.systemDefaultZone());
    }

    MobileAttendanceServiceImpl(
            MobileEmployeeAccessService mobileEmployeeAccessService,
            DailyAttendanceInternalRepository dailyAttendanceInternalRepository,
            FileStorageService fileStorageService,
            HolidayRepository holidayRepository,
            WeekOffWorkingDayRepository weekOffWorkingDayRepository,
            LeaveApplicationRepository leaveApplicationRepository,
            TourApplicationRepository tourApplicationRepository,
            Clock clock) {
        this.mobileEmployeeAccessService = mobileEmployeeAccessService;
        this.dailyAttendanceInternalRepository = dailyAttendanceInternalRepository;
        this.fileStorageService = fileStorageService;
        this.holidayRepository = holidayRepository;
        this.weekOffWorkingDayRepository = weekOffWorkingDayRepository;
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.tourApplicationRepository = tourApplicationRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public MobileAttendanceResponse checkIn(
            Long employeeId,
            BigDecimal latitude,
            BigDecimal longitude,
            String locationAddress,
            MultipartFile image) {
        EmployeeEntity employee = resolveAuthenticatedEmployee(employeeId);
        BigDecimal normalizedLatitude = normalizeLatitude(latitude);
        BigDecimal normalizedLongitude = normalizeLongitude(longitude);
        String normalizedAddress = normalizeLocationAddress(locationAddress);
        validateImage(image);

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate attendanceDate = now.toLocalDate();
        DailyAttendanceInternalEntity attendance = findAttendanceForUpdate(employee, attendanceDate)
                .orElseGet(DailyAttendanceInternalEntity::new);

        if (hasMobileCheckIn(attendance)) {
            throw conflict("ALREADY_CHECKED_IN", "Attendance is already checked in for today.");
        }

        FileUploadResult uploadResult = fileStorageService.store(image, MOBILE_ATTENDANCE_PHOTO_MODULE);

        applyBaseAttendance(attendance, employee, attendanceDate);
        attendance.setCheckInTime(now.toLocalTime());
        attendance.setStatus(PRESENT_STATUS);
        attendance.setCheckInLatitude(normalizedLatitude);
        attendance.setCheckInLongitude(normalizedLongitude);
        attendance.setCheckInLocationAddress(normalizedAddress);
        attendance.setCheckInImagePath(uploadResult.fullPath());
        stampUpdatedBy(attendance);

        DailyAttendanceInternalEntity savedAttendance = dailyAttendanceInternalRepository.save(attendance);
        logAttendanceUpdate(savedAttendance, "MOBILE_APP", checkInUpdatedFields(), "CHECK_IN_RECORDED");
        return toResponse(savedAttendance, "Check-in recorded successfully.");
    }

    @Override
    @Transactional
    public MobileAttendanceResponse checkOut(
            Long employeeId,
            BigDecimal latitude,
            BigDecimal longitude,
            String locationAddress,
            MultipartFile image) {
        EmployeeEntity employee = resolveAuthenticatedEmployee(employeeId);
        BigDecimal normalizedLatitude = normalizeLatitude(latitude);
        BigDecimal normalizedLongitude = normalizeLongitude(longitude);
        String normalizedAddress = normalizeLocationAddress(locationAddress);
        validateImage(image);

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate attendanceDate = now.toLocalDate();
        DailyAttendanceInternalEntity attendance = findAttendanceForUpdate(employee, attendanceDate)
                .orElseThrow(() -> badRequest("CHECK_IN_REQUIRED", "Check-in is required before check-out."));

        if (!hasMobileCheckIn(attendance)) {
            throw badRequest("CHECK_IN_REQUIRED", "Check-in is required before check-out.");
        }
        if (hasMobileCheckOut(attendance)) {
            throw conflict("ALREADY_CHECKED_OUT", "Attendance is already checked out for today.");
        }
        LocalTime checkOutTime = now.toLocalTime();
        if (checkOutTime.isBefore(attendance.getCheckInTime())) {
            throw badRequest("INVALID_CHECK_OUT_TIME", "Check-out time cannot be before check-in time.");
        }

        FileUploadResult uploadResult = fileStorageService.store(image, MOBILE_ATTENDANCE_PHOTO_MODULE);

        applyBaseAttendance(attendance, employee, attendanceDate);
        attendance.setCheckOutTime(checkOutTime);
        attendance.setTotalHours(calculateTotalHours(attendance.getCheckInTime(), checkOutTime));
        attendance.setCheckOutLatitude(normalizedLatitude);
        attendance.setCheckOutLongitude(normalizedLongitude);
        attendance.setCheckOutLocationAddress(normalizedAddress);
        attendance.setCheckOutImagePath(uploadResult.fullPath());
        stampUpdatedBy(attendance);

        DailyAttendanceInternalEntity savedAttendance = dailyAttendanceInternalRepository.save(attendance);
        logAttendanceUpdate(savedAttendance, "MOBILE_APP", checkOutUpdatedFields(), "CHECK_OUT_RECORDED");
        return toResponse(savedAttendance, "Check-out recorded successfully.");
    }

    @Override
    @Transactional(readOnly = true)
    public MobileAttendanceHistoryResponse getHistory(Long employeeId, LocalDate fromDate, LocalDate toDate) {
        EmployeeEntity employee = resolveAuthenticatedEmployee(employeeId);
        LocalDate startDate = requireHistoryStartDate(fromDate);
        LocalDate endDate = toDate != null ? toDate : startDate;
        if (endDate.isBefore(startDate)) {
            throw badRequest("INVALID_DATE_RANGE", "To date cannot be before from date.");
        }

        Map<LocalDate, DailyAttendanceInternalEntity> attendanceByDate = mapAttendanceByDate(
                dailyAttendanceInternalRepository.findByEmployeeIdAndAttendanceDateBetween(
                        employee.getEmployeeId(),
                        startDate,
                        endDate));
        Map<LocalDate, HolidayMasterEntity> holidaysByDate = holidayRepository
                .findByHolidayDateBetween(startDate, endDate)
                .stream()
                .filter(holiday -> holiday.getHolidayDate() != null)
                .collect(Collectors.toMap(
                        HolidayMasterEntity::getHolidayDate,
                        holiday -> holiday,
                        (existing, replacement) -> existing));
        Set<LocalDate> workingDayOverrideDates = weekOffWorkingDayRepository
                .findByWorkingDateBetween(startDate, endDate)
                .stream()
                .map(workingDay -> workingDay.getWorkingDate())
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        List<LeaveApplicationEntity> approvedLeaves = leaveApplicationRepository
                .findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        employee.getEmployeeId(),
                        APPROVED_STATUS,
                        endDate,
                        startDate);
        List<TourApplicationEntity> approvedTours = tourApplicationRepository
                .findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        employee.getEmployeeId(),
                        APPROVED_STATUS,
                        endDate,
                        startDate);

        List<MobileAttendanceHistoryResponse.AttendanceEntry> history = new ArrayList<>();
        LocalDate today = LocalDate.now(clock);
        for (LocalDate date = endDate; !date.isBefore(startDate); date = date.minusDays(1)) {
            history.add(toHistoryRecord(
                    employee,
                    date,
                    attendanceByDate.get(date),
                    holidaysByDate,
                    workingDayOverrideDates,
                    approvedLeaves,
                    approvedTours,
                    today));
        }

        return new MobileAttendanceHistoryResponse(
                true,
                "Attendance history fetched successfully.",
                employee.getEmployeeId(),
                startDate,
                endDate,
                history);
    }

    private EmployeeEntity resolveAuthenticatedEmployee(Long requestedEmployeeId) {
        EmployeeEntity employee = mobileEmployeeAccessService.requireCurrentActiveEmployee(requestedEmployeeId);
        if (!StringUtils.hasText(employee.getEmployeeCode())) {
            throw badRequest("EMPLOYEE_CODE_REQUIRED", "Employee code is not available in employee master.");
        }
        return employee;
    }

    private void applyBaseAttendance(
            DailyAttendanceInternalEntity attendance,
            EmployeeEntity employee,
            LocalDate attendanceDate) {
        attendance.setEmployeeId(employee.getEmployeeId());
        attendance.setEmployeeCode(employee.getEmployeeCode().trim());
        attendance.setAttendanceDate(attendanceDate);
        attendance.setMobileAppStatus("Y");
        attendance.setStatus(PRESENT_STATUS);
        attendance.setMonth(attendanceDate.getMonthValue());
        attendance.setYear(attendanceDate.getYear());
    }

    private void stampUpdatedBy(DailyAttendanceInternalEntity attendance) {
        String actor = currentUsername();
        if (attendance.getCreatedBy() == null) {
            attendance.setCreatedBy(actor);
        }
        attendance.setUpdatedBy(actor);
    }

    private MobileAttendanceResponse toResponse(DailyAttendanceInternalEntity attendance, String message) {
        return new MobileAttendanceResponse(
                true,
                message,
                attendance.getId(),
                attendance.getEmployeeId(),
                attendance.getEmployeeCode(),
                attendance.getAttendanceDate(),
                attendance.getCheckInTime(),
                attendance.getCheckOutTime(),
                resolveSourceLabel(attendance),
                normalizeFlag(attendance.getMobileAppStatus()),
                normalizeFlag(attendance.getApiStatus()));
    }

    private MobileAttendanceHistoryResponse.AttendanceEntry toHistoryRecord(
            EmployeeEntity employee,
            LocalDate date,
            DailyAttendanceInternalEntity attendance,
            Map<LocalDate, HolidayMasterEntity> holidaysByDate,
            Set<LocalDate> workingDayOverrideDates,
            List<LeaveApplicationEntity> approvedLeaves,
            List<TourApplicationEntity> approvedTours,
            LocalDate today) {
        Optional<LeaveApplicationEntity> matchingLeave = hasMobilePunch(attendance)
                ? Optional.empty()
                : findMatchingLeave(approvedLeaves, date);

        if (employee.getJoiningDate() != null && date.isBefore(employee.getJoiningDate())) {
            return toSyntheticHistoryRecord(date, "PRE_JOINING");
        }
        if (matchingLeave.isPresent()) {
            return toSyntheticHistoryRecord(
                    attendance,
                    date,
                    isCompOffLeave(matchingLeave.get()) ? "COMP_OFF" : "LEAVE");
        }
        if (isCoveredByTour(approvedTours, date)) {
            return toSyntheticHistoryRecord(attendance, date, "TOUR");
        }
        if (holidaysByDate.containsKey(date)) {
            return toSyntheticHistoryRecord(attendance, date, "HOLIDAY");
        }
        if (date.isAfter(today)) {
            return toSyntheticHistoryRecord(date, "FUTURE");
        }
        if (isWeekOff(date, workingDayOverrideDates)) {
            return toSyntheticHistoryRecord(date, "WEEK_OFF");
        }
        if (!hasMobilePunch(attendance)) {
            return toSyntheticHistoryRecord(date, "ABSENT");
        }

        String attendanceStatus = resolveAttendanceDisplayStatus(
                attendance,
                workingDayOverrideDates.contains(date));
        if (StringUtils.hasText(attendanceStatus)) {
            return toExistingHistoryRecord(attendance, attendanceStatus);
        }
        return toSyntheticHistoryRecord(date, "ABSENT");
    }

    private MobileAttendanceHistoryResponse.AttendanceEntry toExistingHistoryRecord(
            DailyAttendanceInternalEntity attendance,
            String displayStatus) {
        return new MobileAttendanceHistoryResponse.AttendanceEntry(
                attendance.getId(),
                attendance.getAttendanceDate(),
                attendance.getCheckInTime(),
                attendance.getCheckOutTime(),
                attendance.getCheckInLatitude(),
                attendance.getCheckInLongitude(),
                attendance.getCheckInLocationAddress(),
                attendance.getCheckOutLatitude(),
                attendance.getCheckOutLongitude(),
                attendance.getCheckOutLocationAddress(),
                attendance.getInTime(),
                attendance.getOutTime(),
                resolveTotalHours(attendance),
                displayStatus,
                resolveSourceLabel(attendance),
                normalizeFlag(attendance.getMobileAppStatus()),
                normalizeFlag(attendance.getApiStatus()),
                hasMobileCheckIn(attendance),
                hasMobileCheckOut(attendance));
    }

    private MobileAttendanceHistoryResponse.AttendanceEntry toSyntheticHistoryRecord(LocalDate date, String status) {
        return toSyntheticHistoryRecord(null, date, status);
    }

    private MobileAttendanceHistoryResponse.AttendanceEntry toSyntheticHistoryRecord(
            DailyAttendanceInternalEntity attendance,
            LocalDate date,
            String status) {
        if (attendance == null) {
            return new MobileAttendanceHistoryResponse.AttendanceEntry(
                    null,
                    date,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    status,
                    null,
                    "N",
                    "N",
                    false,
                    false);
        }
        return toExistingHistoryRecord(attendance, status);
    }

    private Map<LocalDate, DailyAttendanceInternalEntity> mapAttendanceByDate(
            Collection<DailyAttendanceInternalEntity> attendanceRows) {
        Map<LocalDate, DailyAttendanceInternalEntity> attendanceByDate = new HashMap<>();
        for (DailyAttendanceInternalEntity attendance : attendanceRows) {
            if (attendance == null || attendance.getAttendanceDate() == null) {
                continue;
            }
            attendanceByDate.merge(
                    attendance.getAttendanceDate(),
                    attendance,
                    this::latestAttendance);
        }
        return attendanceByDate;
    }

    private DailyAttendanceInternalEntity latestAttendance(
            DailyAttendanceInternalEntity first,
            DailyAttendanceInternalEntity second) {
        Long firstId = first.getId();
        Long secondId = second.getId();
        if (firstId == null) {
            return second;
        }
        if (secondId == null) {
            return first;
        }
        return secondId > firstId ? second : first;
    }

    private String resolveAttendanceDisplayStatus(
            DailyAttendanceInternalEntity attendance,
            boolean workingDayOverride) {
        String resolvedStatus = AttendanceStatusResolver.resolveDisplayStatus(attendance);
        if (workingDayOverride && "WEEK_OFF".equals(resolvedStatus)) {
            return null;
        }
        return resolvedStatus;
    }

    private Optional<LeaveApplicationEntity> findMatchingLeave(
            List<LeaveApplicationEntity> approvedLeaves,
            LocalDate date) {
        return approvedLeaves.stream()
                .filter(leave -> isDateCoveredByLeave(leave, date))
                .findFirst();
    }

    private boolean isDateCoveredByLeave(LeaveApplicationEntity leave, LocalDate date) {
        return leave != null
                && leave.getStartDate() != null
                && leave.getEndDate() != null
                && !date.isBefore(leave.getStartDate())
                && !date.isAfter(leave.getEndDate());
    }

    private boolean isCompOffLeave(LeaveApplicationEntity leave) {
        return leave != null
                && (leave.getCompOffWorkDate() != null || isCompOffLeaveType(leave.getLeaveType()));
    }

    private boolean isCompOffLeaveType(String leaveType) {
        if (!StringUtils.hasText(leaveType)) {
            return false;
        }
        String normalized = leaveType.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return normalized.equals("CO")
                || normalized.equals("COMPOFF")
                || normalized.equals("COMPOFFLEAVE")
                || normalized.equals("COMPENSATORYOFF")
                || normalized.equals("COMPENSATORYLEAVE");
    }

    private boolean isCoveredByTour(List<TourApplicationEntity> approvedTours, LocalDate date) {
        return approvedTours.stream()
                .anyMatch(tour -> tour.getStartDate() != null
                        && tour.getEndDate() != null
                        && !date.isBefore(tour.getStartDate())
                        && !date.isAfter(tour.getEndDate()));
    }

    private boolean isWeekOff(LocalDate date, Set<LocalDate> workingDayOverrideDates) {
        return !workingDayOverrideDates.contains(date)
                && (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY);
    }

    private LocalDate requireHistoryStartDate(LocalDate fromDate) {
        if (fromDate == null) {
            throw badRequest("ATTENDANCE_DATE_REQUIRED", "Attendance date is required.");
        }
        return fromDate;
    }

    private String resolveTotalHours(DailyAttendanceInternalEntity attendance) {
        String calculatedTotalHours = calculateTotalHours(attendance.getCheckInTime(), attendance.getCheckOutTime());
        return calculatedTotalHours != null ? calculatedTotalHours : attendance.getTotalHours();
    }

    private Optional<DailyAttendanceInternalEntity> findAttendanceForUpdate(
            EmployeeEntity employee,
            LocalDate attendanceDate) {
        List<DailyAttendanceInternalEntity> rows =
                dailyAttendanceInternalRepository.findByEmployeeIdentityAndAttendanceDateForUpdate(
                        employee.getEmployeeId(),
                        employee.getEmployeeCode().trim(),
                        attendanceDate);
        if (rows.size() > 1) {
            Long keptId = rows.getFirst().getId();
            List<Long> duplicateIds = rows.stream()
                    .skip(1)
                    .map(DailyAttendanceInternalEntity::getId)
                    .toList();
            log.warn(
                    "Duplicate attendance rows found during mobile update. employeeId={}, employeeCode={}, attendanceDate={}, keptAttendanceId={}, duplicateAttendanceIds={}",
                    employee.getEmployeeId(),
                    employee.getEmployeeCode(),
                    attendanceDate,
                    keptId,
                    duplicateIds);
        }
        return rows.stream().findFirst();
    }

    private boolean hasMobileCheckIn(DailyAttendanceInternalEntity attendance) {
        return attendance != null && attendance.getCheckInTime() != null;
    }

    private boolean hasMobileCheckOut(DailyAttendanceInternalEntity attendance) {
        return attendance != null && attendance.getCheckOutTime() != null;
    }

    private boolean hasMobilePunch(DailyAttendanceInternalEntity attendance) {
        return attendance != null
                && isMobileAttendanceMarked(attendance)
                && (attendance.getCheckInTime() != null || attendance.getCheckOutTime() != null);
    }

    private boolean isMobileAttendanceMarked(DailyAttendanceInternalEntity attendance) {
        return attendance != null
                && ("Y".equalsIgnoreCase(attendance.getMobileAppStatus())
                        || attendance.getCheckInTime() != null
                        || attendance.getCheckOutTime() != null);
    }

    private boolean isApiAttendanceMarked(DailyAttendanceInternalEntity attendance) {
        return attendance != null
                && ("Y".equalsIgnoreCase(attendance.getApiStatus())
                        || StringUtils.hasText(attendance.getInTime())
                        || StringUtils.hasText(attendance.getOutTime()));
    }

    private String resolveSourceLabel(DailyAttendanceInternalEntity attendance) {
        if (isMobileAttendanceMarked(attendance)) {
            return "MOBILE_APP";
        }
        if (isApiAttendanceMarked(attendance)) {
            return "API";
        }
        return null;
    }

    private List<String> checkInUpdatedFields() {
        return List.of(
                "mobile_app_status",
                "check_in_time",
                "status",
                "check_in_latitude",
                "check_in_longitude",
                "check_in_location_address",
                "check_in_image_path");
    }

    private List<String> checkOutUpdatedFields() {
        return List.of(
                "mobile_app_status",
                "check_out_time",
                "total_hours",
                "check_out_latitude",
                "check_out_longitude",
                "check_out_location_address",
                "check_out_image_path");
    }

    private void logAttendanceUpdate(
            DailyAttendanceInternalEntity attendance,
            String sourceType,
            List<String> updatedFields,
            String result) {
        log.info(
                "Attendance update completed. employeeId={}, employeeCode={}, attendanceDate={}, sourceType={}, updatedFields={}, result={}",
                attendance.getEmployeeId(),
                attendance.getEmployeeCode(),
                attendance.getAttendanceDate(),
                sourceType,
                updatedFields,
                result);
    }

    private String normalizeFlag(String value) {
        return "Y".equalsIgnoreCase(value) ? "Y" : "N";
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw badRequest("IMAGE_REQUIRED", "Attendance image is required.");
        }
    }

    private BigDecimal normalizeLatitude(BigDecimal latitude) {
        return normalizeCoordinate(latitude, MIN_LATITUDE, MAX_LATITUDE, "latitude", "Latitude");
    }

    private BigDecimal normalizeLongitude(BigDecimal longitude) {
        return normalizeCoordinate(longitude, MIN_LONGITUDE, MAX_LONGITUDE, "longitude", "Longitude");
    }

    private BigDecimal normalizeCoordinate(
            BigDecimal value,
            BigDecimal minimum,
            BigDecimal maximum,
            String codeField,
            String displayField) {
        if (value == null) {
            throw badRequest(codeField.toUpperCase(Locale.ROOT) + "_REQUIRED", displayField + " is required.");
        }
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw badRequest(
                    "INVALID_" + codeField.toUpperCase(Locale.ROOT),
                    displayField + " is outside the allowed range.");
        }
        return value.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
    }

    private String normalizeLocationAddress(String locationAddress) {
        String normalized = StringUtils.hasText(locationAddress) ? locationAddress.trim() : null;
        if (normalized != null && normalized.length() > LOCATION_ADDRESS_MAX_LENGTH) {
            throw badRequest("LOCATION_ADDRESS_TOO_LONG", "Location address must not exceed 1000 characters.");
        }
        return normalized;
    }

    private String calculateTotalHours(LocalTime checkInTime, LocalTime checkOutTime) {
        if (checkInTime == null || checkOutTime == null || checkOutTime.isBefore(checkInTime)) {
            return null;
        }

        Duration duration = Duration.between(checkInTime, checkOutTime);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return "%02d:%02d".formatted(hours, minutes);
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && StringUtils.hasText(authentication.getName())
                ? authentication.getName().trim()
                : "mobile-app";
    }

    private MobileAttendanceException badRequest(String code, String message) {
        return new MobileAttendanceException(HttpStatus.BAD_REQUEST, code, message);
    }

    private MobileAttendanceException conflict(String code, String message) {
        return new MobileAttendanceException(HttpStatus.CONFLICT, code, message);
    }
}
