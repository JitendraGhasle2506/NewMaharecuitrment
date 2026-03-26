package com.maharecruitment.gov.in.attendance.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.attendance.dto.TourApplicationHODDTO;
import com.maharecruitment.gov.in.attendance.entity.TourApplicationEntity;
import com.maharecruitment.gov.in.attendance.repository.TourApplicationRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingMappingRepository;

@Service
@Transactional
public class TourApplicationServiceImpl implements TourApplicationService {

    @Autowired
    private TourApplicationRepository tourApplicationRepository;

    @Autowired
    private EmployeeReportingMappingRepository employeeReportingMappingRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

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
    public List<TourApplicationEntity> getTourApplicationsByEmployee(Long employeeId) {
        return tourApplicationRepository.findByEmployeeIdOrderByApplicationDateDesc(employeeId);
    }

    @Override
    public List<TourApplicationHODDTO> getPendingToursForHOD(Long hodUserId) {
        List<EmployeeReportingMappingEntity> mappings = employeeReportingMappingRepository.findByHodUserId(hodUserId);
        if (mappings.isEmpty()) {
            return List.of();
        }
        List<Long> employeeIds = mappings.stream()
                .map(EmployeeReportingMappingEntity::getEmployeeId)
                .collect(Collectors.toList());
        
        List<TourApplicationEntity> tours = tourApplicationRepository.findByEmployeeIdInAndStatusOrderByApplicationDateDesc(employeeIds, "PENDING");
        
        if (tours.isEmpty()) {
            return List.of();
        }

        Map<Long, EmployeeEntity> employeeMap = employeeRepository.findAllById(employeeIds).stream()
                .collect(Collectors.toMap(EmployeeEntity::getEmployeeId, emp -> emp));

        List<TourApplicationHODDTO> dtos = new ArrayList<>();
        for (TourApplicationEntity tour : tours) {
            EmployeeEntity emp = employeeMap.get(tour.getEmployeeId());
            TourApplicationHODDTO dto = new TourApplicationHODDTO();
            dto.setTourId(tour.getTourId());
            dto.setEmployeeId(tour.getEmployeeId());
            dto.setEmployeeCode(emp != null ? emp.getEmployeeCode() : "");
            dto.setEmployeeName(emp != null ? emp.getFullName() : "Unknown");
            dto.setTourCategory(tour.getTourCategory());
            dto.setTimePeriod(tour.getTimePeriod());
            dto.setStartDate(tour.getStartDate());
            dto.setEndDate(tour.getEndDate());
            dto.setDescription(tour.getDescription());
            dto.setApplicationDate(tour.getApplicationDate());
            dto.setStatus(tour.getStatus());
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
