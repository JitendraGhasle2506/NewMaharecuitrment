package com.maharecruitment.gov.in.attendance.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.attendance.dto.TourApplicationHODDTO;
import com.maharecruitment.gov.in.attendance.entity.TourApplicationEntity;
import com.maharecruitment.gov.in.attendance.repository.TourApplicationRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.service.ReportingManagerService;

@Service
@Transactional
public class TourApplicationServiceImpl implements TourApplicationService {

    private final TourApplicationRepository tourApplicationRepository;
    private final ReportingManagerService reportingManagerService;
    private final EmployeeRepository employeeRepository;

    public TourApplicationServiceImpl(
            TourApplicationRepository tourApplicationRepository,
            ReportingManagerService reportingManagerService,
            EmployeeRepository employeeRepository) {
        this.tourApplicationRepository = tourApplicationRepository;
        this.reportingManagerService = reportingManagerService;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public void saveTourApplication(TourApplicationEntity tourApplication) {
        if (tourApplication.getApplicationDate() == null) {
            tourApplication.setApplicationDate(LocalDateTime.now());
        }
        if (tourApplication.getStatus() == null) {
            tourApplication.setStatus("PENDING");
        }
        tourApplicationRepository.save(tourApplication);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourApplicationEntity> getTourApplicationsByEmployee(Long employeeId) {
        return tourApplicationRepository.findByEmployeeIdOrderByApplicationDateDesc(employeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourApplicationHODDTO> getPendingToursForHOD(Long hodUserId, String search) {
        List<Long> employeeIds = reportingManagerService.getEffectiveEmployeeIdsForAuthority(hodUserId);
        if (employeeIds.isEmpty()) {
            return List.of();
        }

        List<TourApplicationEntity> tours = tourApplicationRepository
                .findByEmployeeIdInAndStatusOrderByApplicationDateDesc(employeeIds, "PENDING");
        
        if (tours.isEmpty()) {
            return List.of();
        }

        return convertToHODDTO(tours, employeeIds, search);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourApplicationHODDTO> getProcessedToursForHOD(Long hodUserId, String search) {
        List<Long> employeeIds = reportingManagerService.getEffectiveEmployeeIdsForAuthority(hodUserId);
        if (employeeIds.isEmpty()) {
            return List.of();
        }

        List<TourApplicationEntity> tours = tourApplicationRepository
                .findByEmployeeIdInAndStatusInOrderByApplicationDateDesc(
                        employeeIds,
                        List.of("APPROVED", "REJECTED"));
        
        if (tours.isEmpty()) {
            return List.of();
        }

        return convertToHODDTO(tours, employeeIds, search);
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
    public void updateTourStatus(Long tourId, String status, String remarks) {
        TourApplicationEntity tour = tourApplicationRepository.findById(tourId).orElse(null);
        if (tour != null) {
            tour.setStatus(status);
            tour.setHodRemarks(remarks);
            tourApplicationRepository.save(tour);
        }
    }
}
