package com.maharecruitment.gov.in.web.service.hr;

import java.util.List;
import com.maharecruitment.gov.in.recruitment.dto.employee.EmployeeRelievingDto;

public interface EmployeeRelievingService {
    List<EmployeeRelievingDto> getAllRelievingRecords();
    EmployeeRelievingDto getRelievingById(Long relievingId);
    void saveRelieving(EmployeeRelievingDto dto);
}
