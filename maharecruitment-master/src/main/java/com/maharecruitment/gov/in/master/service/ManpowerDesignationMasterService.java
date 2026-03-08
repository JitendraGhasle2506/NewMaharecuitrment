package com.maharecruitment.gov.in.master.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.master.dto.ManpowerDesignationMasterRequest;
import com.maharecruitment.gov.in.master.dto.ManpowerDesignationMasterResponse;

public interface ManpowerDesignationMasterService {

    ManpowerDesignationMasterResponse create(ManpowerDesignationMasterRequest request);

    ManpowerDesignationMasterResponse update(Long designationId, ManpowerDesignationMasterRequest request);

    ManpowerDesignationMasterResponse getById(Long designationId, boolean includeInactive);

    Page<ManpowerDesignationMasterResponse> getAll(boolean includeInactive, Pageable pageable);

    void softDelete(Long designationId);

    void restore(Long designationId);
}

