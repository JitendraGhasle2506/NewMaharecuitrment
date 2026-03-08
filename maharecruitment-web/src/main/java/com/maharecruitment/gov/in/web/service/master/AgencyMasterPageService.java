package com.maharecruitment.gov.in.web.service.master;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.master.dto.AgencyMasterResponse;
import com.maharecruitment.gov.in.master.entity.AgencyStatus;
import com.maharecruitment.gov.in.web.dto.master.AgencyMasterForm;

public interface AgencyMasterPageService {

    Page<AgencyMasterResponse> getAll(Pageable pageable);

    AgencyMasterResponse getById(Long agencyId);

    AgencyMasterResponse create(AgencyMasterForm form);

    AgencyMasterResponse update(Long agencyId, AgencyMasterForm form);

    AgencyMasterResponse updateStatus(Long agencyId, AgencyStatus status);
}
