package com.maharecruitment.gov.in.attendance.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.attendance.dto.LeaveApplicationHODDTO;
import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;
import com.maharecruitment.gov.in.attendance.repository.AttendanceRegisterRepo;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.attendance.repository.LeaveApplicationRepository;
import com.maharecruitment.gov.in.attendance.repository.TourApplicationRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.service.ReportingManagerService;

@Service
@Transactional
public class LeaveApplicationServiceImpl implements LeaveApplicationService {
    private static final List<String> ACTIVE_APPLICATION_STATUSES = List.of("PENDING", "APPROVED", "ACCEPTED");
    private static final List<String> APPROVED_APPLICATION_STATUSES = List.of("APPROVED", "ACCEPTED");

    @Autowired
    private LeaveApplicationRepository leaveApplicationRepository;

    @Autowired
    private DailyAttendanceInternalRepository dailyAttendanceInternalRepository;

    @Autowired
    private AttendanceRegisterRepo attendanceRegisterRepo;

    @Autowired
    private TourApplicationRepository tourApplicationRepository;

    @Autowired
    private ReportingManagerService reportingManagerService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public void saveLeaveApplication(LeaveApplicationEntity leaveApplication) {
        prepareAndValidateLeaveApplication(leaveApplication);
        if (leaveApplication.getApplicationDate() == null) {
            leaveApplication.setApplicationDate(LocalDateTime.now());
        }
        if (leaveApplication.getStatus() == null) {
            leaveApplication.setStatus("PENDING");
        }
        leaveApplicationRepository.save(leaveApplication);
    }

    private void prepareAndValidateLeaveApplication(LeaveApplicationEntity leaveApplication) {
        if (leaveApplication == null) {
            throw new IllegalArgumentException("Leave application details are required.");
        }
        leaveApplication.setLeaveType(trim(leaveApplication.getLeaveType()));
        leaveApplication.setLeaveCategory(trim(leaveApplication.getLeaveCategory()));
        leaveApplication.setDescription(trim(leaveApplication.getDescription()));

        if (!StringUtils.hasText(leaveApplication.getLeaveType())) {
            throw new IllegalArgumentException("Please select leave type.");
        }
        if (!StringUtils.hasText(leaveApplication.getLeaveCategory())) {
            throw new IllegalArgumentException("Please select leave category.");
        }
        if (leaveApplication.getStartDate() == null || leaveApplication.getEndDate() == null) {
            throw new IllegalArgumentException("Please select leave start date and end date.");
        }
        if (leaveApplication.getEndDate().isBefore(leaveApplication.getStartDate())) {
            throw new IllegalArgumentException("End date must be greater than or equal to start date.");
        }
        if (isCompOffLeave(leaveApplication.getLeaveType())) {
            validateCompOffRequest(leaveApplication);
        } else {
            leaveApplication.setCompOffWorkDate(null);
        }
    }

    private void validateCompOffRequest(LeaveApplicationEntity leaveApplication) {
        LocalDate compOffWorkDate = leaveApplication.getCompOffWorkDate();
        if (compOffWorkDate == null) {
            throw new IllegalArgumentException("Please select the worked date for comp-off leave.");
        }
        if (compOffWorkDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Comp-off worked date cannot be a future date.");
        }
        if (!compOffWorkDate.isBefore(leaveApplication.getStartDate())) {
            throw new IllegalArgumentException("Comp-off worked date must be before the leave start date.");
        }
        boolean alreadyUsed = leaveApplicationRepository.existsByEmployeeIdAndCompOffWorkDateAndStatusIn(
                leaveApplication.getEmployeeId(),
                compOffWorkDate,
                ACTIVE_APPLICATION_STATUSES);
        if (alreadyUsed) {
            throw new IllegalArgumentException("A pending or approved comp-off request already exists for this worked date.");
        }
        if (!isValidCompOffWorkedDate(leaveApplication.getEmployeeId(), compOffWorkDate)) {
            throw new IllegalArgumentException(
                    "Comp-off worked date is allowed only when you were present or had an approved tour on that date.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isValidCompOffWorkedDate(Long employeeId, LocalDate workedDate) {
        if (employeeId == null || workedDate == null) {
            return false;
        }
        return hasInternalPresentAttendance(employeeId, workedDate)
                || hasExternalPresentAttendance(employeeId, workedDate)
                || hasApprovedTour(employeeId, workedDate);
    }

    private boolean hasInternalPresentAttendance(Long employeeId, LocalDate workedDate) {
        return dailyAttendanceInternalRepository.findByEmployeeIdAndAttendanceDate(employeeId, workedDate)
                .map(attendance -> "PRESENT".equalsIgnoreCase(trim(attendance.getStatus()))
                        || StringUtils.hasText(attendance.getInTime())
                        || StringUtils.hasText(attendance.getOutTime()))
                .orElse(false);
    }

    private boolean hasExternalPresentAttendance(Long employeeId, LocalDate workedDate) {
        return attendanceRegisterRepo.findDayStatus(
                employeeId,
                workedDate.getMonthValue(),
                workedDate.getYear(),
                workedDate.getDayOfMonth())
                .map(this::isPresentStatus)
                .orElse(false);
    }

    private boolean hasApprovedTour(Long employeeId, LocalDate workedDate) {
        return tourApplicationRepository.existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                employeeId,
                APPROVED_APPLICATION_STATUSES,
                workedDate,
                workedDate);
    }

    private boolean isPresentStatus(String status) {
        String normalized = normalizeStatus(status);
        return normalized.equals("P") || normalized.equals("PRESENT");
    }

    private String normalizeStatus(String status) {
        return trim(status) == null ? "" : trim(status).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private boolean isCompOffLeave(String leaveType) {
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

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    @Override
    public List<LeaveApplicationEntity> getLeaveApplicationsByEmployee(Long employeeId) {
        return leaveApplicationRepository.findByEmployeeIdOrderByApplicationDateDesc(employeeId);
    }

    @Override
    public List<LeaveApplicationHODDTO> getPendingLeavesForHOD(Long hodUserId, String search) {
        List<Long> employeeIds = reportingManagerService.getEffectiveEmployeeIdsForAuthority(hodUserId);
        if (employeeIds.isEmpty()) {
            return List.of();
        }

        List<LeaveApplicationEntity> leaves = leaveApplicationRepository.findByEmployeeIdInAndStatusOrderByApplicationDateDesc(employeeIds, "PENDING");
        
        if (leaves.isEmpty()) {
            return List.of();
        }

        return convertToHODDTO(leaves, employeeIds, search);
    }

    @Override
    public List<LeaveApplicationHODDTO> getProcessedLeavesForHOD(Long hodUserId, String search) {
        List<Long> employeeIds = reportingManagerService.getEffectiveEmployeeIdsForAuthority(hodUserId);
        if (employeeIds.isEmpty()) {
            return List.of();
        }

        List<LeaveApplicationEntity> leaves = leaveApplicationRepository.findByEmployeeIdInAndStatusInOrderByApplicationDateDesc(employeeIds, List.of("APPROVED", "REJECTED"));
        
        if (leaves.isEmpty()) {
            return List.of();
        }

        return convertToHODDTO(leaves, employeeIds, search);
    }

    private List<LeaveApplicationHODDTO> convertToHODDTO(List<LeaveApplicationEntity> leaves, List<Long> employeeIds, String search) {
        Map<Long, EmployeeEntity> employeeMap = employeeRepository.findAllById(employeeIds).stream()
                .collect(Collectors.toMap(EmployeeEntity::getEmployeeId, emp -> emp));

        List<LeaveApplicationHODDTO> dtos = new ArrayList<>();
        for (LeaveApplicationEntity leave : leaves) {
            EmployeeEntity emp = employeeMap.get(leave.getEmployeeId());
            
            // Filter by search query (Name or Designation)
            if (search != null && !search.trim().isEmpty()) {
                String query = search.toLowerCase().trim();
                boolean matchesName = emp != null && emp.getFullName() != null && emp.getFullName().toLowerCase().contains(query);
                
                String designationName = (emp != null && emp.getDesignation() != null) ? emp.getDesignation().getDesignationName() : null;
                boolean matchesDesignation = designationName != null && designationName.toLowerCase().contains(query);
                
                if (!matchesName && !matchesDesignation) {
                    continue;
                }
            }

            LeaveApplicationHODDTO dto = new LeaveApplicationHODDTO();
            dto.setLeaveId(leave.getLeaveId());
            dto.setEmployeeId(leave.getEmployeeId());
            dto.setEmployeeCode(emp != null ? emp.getEmployeeCode() : "");
            dto.setEmployeeName(emp != null ? emp.getFullName() : "Unknown");
            
            String designationName = (emp != null && emp.getDesignation() != null) ? emp.getDesignation().getDesignationName() : "";
            dto.setDesignation(designationName);
            
            dto.setLeaveType(leave.getLeaveType());
            dto.setLeaveCategory(leave.getLeaveCategory());
            dto.setStartDate(leave.getStartDate());
            dto.setEndDate(leave.getEndDate());
            dto.setCompOffWorkDate(leave.getCompOffWorkDate());
            dto.setDescription(leave.getDescription());
            dto.setApplicationDate(leave.getApplicationDate());
            dto.setStatus(leave.getStatus());
            dto.setHodRemarks(leave.getHodRemarks());
            dtos.add(dto);
        }
        return dtos;
    }

    @Override
    public void updateLeaveStatus(Long leaveId, String status, String remarks) {
        LeaveApplicationEntity leave = leaveApplicationRepository.findById(leaveId).orElse(null);
        if (leave != null) {
            leave.setStatus(status);
            leave.setHodRemarks(remarks);
            leaveApplicationRepository.save(leave);
        }
    }

    @Override
    public void cancelLeaveApplication(Long leaveId, Long employeeId) {
        LeaveApplicationEntity leave = leaveApplicationRepository.findById(leaveId)
                .orElseThrow(() -> new IllegalArgumentException("Leave application not found."));
        if (!employeeId.equals(leave.getEmployeeId())) {
            throw new IllegalArgumentException("You are not allowed to cancel this leave application.");
        }
        if (!"PENDING".equalsIgnoreCase(trim(leave.getStatus()))) {
            throw new IllegalArgumentException("Only pending leave applications can be cancelled.");
        }
        leave.setStatus("CANCELLED");
        leave.setManagerRemarks("Cancelled by employee before approval.");
        leaveApplicationRepository.save(leave);
    }
}
