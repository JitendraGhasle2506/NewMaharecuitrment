package com.maharecruitment.gov.in.master.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.master.dto.AgencyMasterRequest;
import com.maharecruitment.gov.in.master.dto.AgencyMasterResponse;
import com.maharecruitment.gov.in.master.entity.AgencyStatus;

public interface AgencyMasterService {

    AgencyMasterResponse create(AgencyMasterRequest request);

    AgencyMasterResponse update(Long agencyId, AgencyMasterRequest request);

    AgencyMasterResponse updateStatus(Long agencyId, AgencyStatus status);

    AgencyMasterResponse delete(Long agencyId);

    AgencyMasterResponse getById(Long agencyId);

    Page<AgencyMasterResponse> getAll(Pageable pageable);
}
