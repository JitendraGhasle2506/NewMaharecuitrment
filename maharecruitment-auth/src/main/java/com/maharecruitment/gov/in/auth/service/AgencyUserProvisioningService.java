package com.maharecruitment.gov.in.auth.service;

import com.maharecruitment.gov.in.auth.dto.AgencyUserProvisioningRequest;
import com.maharecruitment.gov.in.auth.dto.AgencyUserProvisioningResult;

public interface AgencyUserProvisioningService {

    AgencyUserProvisioningResult createOrSyncAgencyUser(AgencyUserProvisioningRequest request);
}
