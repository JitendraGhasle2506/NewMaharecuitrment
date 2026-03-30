package com.maharecruitment.gov.in.attendance.service;

import java.util.List;
import com.maharecruitment.gov.in.attendance.dto.TourApplicationHODDTO;
import com.maharecruitment.gov.in.attendance.entity.TourApplicationEntity;

public interface TourApplicationService {
    void saveTourApplication(TourApplicationEntity tourApplication);
    List<TourApplicationEntity> getTourApplicationsByEmployee(Long employeeId);
    List<TourApplicationHODDTO> getPendingToursForHOD(Long hodUserId);
    void updateTourStatus(Long tourId, String status, String remarks);
}
