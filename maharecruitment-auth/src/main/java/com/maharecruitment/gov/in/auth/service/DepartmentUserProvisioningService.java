package com.maharecruitment.gov.in.auth.service;

import com.maharecruitment.gov.in.auth.dto.DepartmentUserProvisioningRequest;
import com.maharecruitment.gov.in.auth.dto.DepartmentUserProvisioningResult;

public interface DepartmentUserProvisioningService {

    DepartmentUserProvisioningResult createDepartmentUser(DepartmentUserProvisioningRequest request);
}
