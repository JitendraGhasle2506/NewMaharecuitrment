package com.maharecruitment.gov.in.attendance.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.attendance.dto.TourApplicationHODDTO;
import com.maharecruitment.gov.in.attendance.entity.TourApplicationEntity;
import com.maharecruitment.gov.in.attendance.repository.TourApplicationRepository;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.service.ReportingManagerService;

@Service
@Transactional
public class TourApplicationServiceImpl implements TourApplicationService {

    private final TourApplicationRepository tourApplicationRepository;
    private final ReportingManagerService reportingManagerService;
    private final EmployeeRepository employeeRepository;
    private final RecruitmentTypeApprovalRoutingService approvalRoutingService;
    private final UserRepository userRepository;

    public TourApplicationServiceImpl(
            TourApplicationRepository tourApplicationRepository,
            ReportingManagerService reportingManagerService,
            EmployeeRepository employeeRepository,
            RecruitmentTypeApprovalRoutingService approvalRoutingService,
            UserRepository userRepository) {
        this.tourApplicationRepository = tourApplicationRepository;
        this.reportingManagerService = reportingManagerService;
        this.employeeRepository = employeeRepository;
        this.approvalRoutingService = approvalRoutingService;
        this.userRepository = userRepository;
    }

    @Override
    public void saveTourApplication(TourApplicationEntity tourApplication) {
        if (tourApplication.getApplicationDate() == null) {
            tourApplication.setApplicationDate(LocalDateTime.now());
        }
        if (tourApplication.getStatus() == null) {
            tourApplication.setStatus("PENDING");
        }
        RecruitmentTypeApprovalRoutingService.ApprovalRoute route =
                approvalRoutingService.routeNewRequest(tourApplication.getEmployeeId());
        tourApplication.setApprovalStage(route.initialStage());
        tourApplication.setManagerApproverUserId(route.managerUserId());
        tourApplication.setHodApproverUserId(route.hodUserId());
        tourApplicationRepository.save(tourApplication);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourApplicationEntity> getTourApplicationsByEmployee(Long employeeId) {
        List<TourApplicationEntity> applications =
                tourApplicationRepository.findByEmployeeIdOrderByApplicationDateDesc(employeeId);
        populateApproverNames(applications);
        return applications;
    }

    private void populateApproverNames(List<TourApplicationEntity> applications) {
        LinkedHashSet<Long> approverIds = new LinkedHashSet<>();
        for (TourApplicationEntity application : applications) {
            if (application.getManagerApproverUserId() != null) {
                approverIds.add(application.getManagerApproverUserId());
            }
            if (application.getHodApproverUserId() != null) {
                approverIds.add(application.getHodApproverUserId());
            }
        }
        if (approverIds.isEmpty()) {
            return;
        }

        Map<Long, String> approverNames = userRepository.findAllById(approverIds).stream()
                .collect(Collectors.toMap(User::getId, this::approverDisplayName));
        for (TourApplicationEntity application : applications) {
            application.setManagerApproverName(
                    approverNames.get(application.getManagerApproverUserId()));
            application.setHodApproverName(
                    approverNames.get(application.getHodApproverUserId()));
        }
    }

    private String approverDisplayName(User user) {
        return user.getName() == null || user.getName().isBlank()
                ? "Approver"
                : user.getName().trim();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourApplicationHODDTO> getPendingToursForHOD(Long hodUserId, String search) {
        List<Long> legacyEmployeeIds = legacyEmployeeIds(hodUserId);
        List<TourApplicationEntity> tours = tourApplicationRepository
                .findPendingForApprover(hodUserId, legacyEmployeeIds, "PENDING");
        
        if (tours.isEmpty()) {
            return List.of();
        }

        return convertToHODDTO(tours, applicationEmployeeIds(tours), search);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourApplicationHODDTO> getPendingToursForHOD(
            Long hodUserId, String search, LocalDate searchDate) {
        List<TourApplicationEntity> tours = tourApplicationRepository.findPendingForApproverFiltered(
                hodUserId,
                legacyEmployeeIds(hodUserId),
                searchPattern(search),
                searchDate);
        return tours.isEmpty()
                ? List.of()
                : convertToHODDTO(tours, applicationEmployeeIds(tours), null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourApplicationHODDTO> getProcessedToursForHOD(Long hodUserId, String search) {
        List<TourApplicationEntity> tours = tourApplicationRepository
                .findProcessedForApprover(
                        hodUserId,
                        legacyEmployeeIds(hodUserId),
                        List.of("APPROVED", "REJECTED"));
        
        if (tours.isEmpty()) {
            return List.of();
        }

        return convertToHODDTO(tours, applicationEmployeeIds(tours), search);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TourApplicationHODDTO> getProcessedToursForHOD(
            Long hodUserId, String search, LocalDate searchDate, Pageable pageable) {
        Page<TourApplicationEntity> page = tourApplicationRepository.findProcessedForApproverPage(
                hodUserId,
                legacyEmployeeIds(hodUserId),
                searchPattern(search),
                searchDate,
                pageable);
        if (page.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, page.getTotalElements());
        }
        List<TourApplicationHODDTO> content = convertToHODDTO(
                page.getContent(), applicationEmployeeIds(page.getContent()), null);
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    private String searchPattern(String search) {
        return search == null || search.isBlank()
                ? null
                : "%" + search.trim().toUpperCase(Locale.ROOT) + "%";
    }

    private List<Long> legacyEmployeeIds(Long actorUserId) {
        List<Long> employeeIds = reportingManagerService.getEffectiveEmployeeIdsForAuthority(actorUserId);
        return employeeIds.isEmpty() ? List.of(-1L) : employeeIds;
    }

    private List<Long> applicationEmployeeIds(List<TourApplicationEntity> tours) {
        return tours.stream().map(TourApplicationEntity::getEmployeeId).distinct().toList();
    }

    private List<TourApplicationHODDTO> convertToHODDTO(List<TourApplicationEntity> tours, List<Long> employeeIds, String search) {
        Map<Long, EmployeeEntity> employeeMap = employeeRepository
                .findByEmployeeIdInOrderByFullNameAscEmployeeIdAsc(employeeIds).stream()
                .collect(Collectors.toMap(EmployeeEntity::getEmployeeId, emp -> emp));
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

        List<TourApplicationHODDTO> dtos = new ArrayList<>();
        for (TourApplicationEntity tour : tours) {
            EmployeeEntity emp = employeeMap.get(tour.getEmployeeId());

            // Filter by search query (Name or Designation)
            if (!normalizedSearch.isEmpty()) {
                boolean matchesName = emp != null
                        && emp.getFullName() != null
                        && emp.getFullName().toLowerCase(Locale.ROOT).contains(normalizedSearch);
                
                String designationName = (emp != null && emp.getDesignation() != null) ? emp.getDesignation().getDesignationName() : null;
                boolean matchesDesignation = designationName != null
                        && designationName.toLowerCase(Locale.ROOT).contains(normalizedSearch);
                
                if (!matchesName && !matchesDesignation) {
                    continue;
                }
            }

            TourApplicationHODDTO dto = new TourApplicationHODDTO();
            dto.setTourId(tour.getTourId());
            dto.setEmployeeId(tour.getEmployeeId());
            dto.setEmployeeCode(emp != null ? emp.getEmployeeCode() : "");
            dto.setEmployeeName(emp != null ? emp.getFullName() : "Unknown");
            
            String designationName = (emp != null && emp.getDesignation() != null) ? emp.getDesignation().getDesignationName() : "";
            dto.setDesignation(designationName);
            
            dto.setTourCategory(tour.getTourCategory());
            dto.setTimePeriod(tour.getTimePeriod());
            dto.setStartDate(tour.getStartDate());
            dto.setEndDate(tour.getEndDate());
            dto.setDescription(tour.getDescription());
            dto.setApplicationDate(tour.getApplicationDate());
            dto.setStatus(tour.getStatus());
            dto.setHodRemarks(tour.getHodRemarks());
            dtos.add(dto);
        }
        return dtos;
    }

    @Override
    public void updateTourStatus(Long tourId, String status, String remarks, Long actorUserId) {
        requireValidDecision(status);
        TourApplicationEntity tour = tourApplicationRepository.findByTourIdForUpdate(tourId)
                .orElseThrow(() -> new IllegalArgumentException("Tour application not found."));
        if (!"PENDING".equalsIgnoreCase(normalize(tour.getStatus()))) {
            throw new IllegalArgumentException("Only pending tour applications can be processed.");
        }

        if (!approvalRoutingService.isTwoLevelStage(tour.getApprovalStage())) {
            if (!reportingManagerService.getEffectiveEmployeeIdsForAuthority(actorUserId)
                    .contains(tour.getEmployeeId())) {
                throw new IllegalArgumentException("This tour application is outside your reporting authority.");
            }
            tour.setStatus(status);
            tour.setHodRemarks(remarks);
        } else {
            if (!approvalRoutingService.canAct(
                    actorUserId,
                    tour.getApprovalStage(),
                    tour.getManagerApproverUserId(),
                    tour.getHodApproverUserId())) {
                throw new IllegalArgumentException("This tour application is not pending at your approval level.");
            }
            applyStagedDecision(tour, status, remarks);
        }
        tourApplicationRepository.save(tour);
    }

    private void applyStagedDecision(TourApplicationEntity tour, String status, String remarks) {
        if (RecruitmentTypeApprovalRoutingService.STAGE_MANAGER.equalsIgnoreCase(tour.getApprovalStage())) {
            tour.setManagerRemarks(remarks);
            if ("APPROVED".equalsIgnoreCase(status)) {
                tour.setApprovalStage(RecruitmentTypeApprovalRoutingService.STAGE_HOD);
                return;
            }
        } else {
            tour.setHodRemarks(remarks);
        }
        tour.setStatus(status);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private void requireValidDecision(String status) {
        if (!"APPROVED".equalsIgnoreCase(normalize(status))
                && !"REJECTED".equalsIgnoreCase(normalize(status))) {
            throw new IllegalArgumentException("Decision must be APPROVED or REJECTED.");
        }
    }
}
