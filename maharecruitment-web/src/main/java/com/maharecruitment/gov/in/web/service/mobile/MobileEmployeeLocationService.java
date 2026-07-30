package com.maharecruitment.gov.in.web.service.mobile;

import com.maharecruitment.gov.in.web.dto.mobile.MobileEmployeeLocationResponse;

public interface MobileEmployeeLocationService {

    MobileEmployeeLocationResponse getMappedLocations(Long employeeId);
}
