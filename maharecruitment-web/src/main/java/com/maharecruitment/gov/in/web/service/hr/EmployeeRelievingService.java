package com.maharecruitment.gov.in.web.service.hr;

import java.util.List;
import java.time.LocalDate;
import com.maharecruitment.gov.in.recruitment.dto.employee.EmployeeRelievingDto;

public interface EmployeeRelievingService {
    List<EmployeeRelievingDto> getAllRelievingRecords();
    List<EmployeeRelievingDto> getRelievingRecordsByAgency(Long agencyId);
    List<EmployeeRelievingDto> getRelievingRecordsByDepartment(Long departmentId);
    EmployeeRelievingDto getRelievingById(Long relievingId);
    void saveRelieving(EmployeeRelievingDto dto);
    void markExitDate(Long relievingId, LocalDate exitDate);
}
