package com.maharecruitment.gov.in.attendance.service;

import java.util.List;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.attendance.dto.TourApplicationHODDTO;
import com.maharecruitment.gov.in.attendance.entity.TourApplicationEntity;

public interface TourApplicationService {
    void saveTourApplication(TourApplicationEntity tourApplication);
    List<TourApplicationEntity> getTourApplicationsByEmployee(Long employeeId);
    List<TourApplicationHODDTO> getPendingToursForHOD(Long hodUserId, String search);
    List<TourApplicationHODDTO> getPendingToursForHOD(Long hodUserId, String search, LocalDate searchDate);
    List<TourApplicationHODDTO> getProcessedToursForHOD(Long hodUserId, String search);
    Page<TourApplicationHODDTO> getProcessedToursForHOD(
            Long hodUserId, String search, LocalDate searchDate, Pageable pageable);
    void updateTourStatus(Long tourId, String status, String remarks, Long actorUserId);
}
