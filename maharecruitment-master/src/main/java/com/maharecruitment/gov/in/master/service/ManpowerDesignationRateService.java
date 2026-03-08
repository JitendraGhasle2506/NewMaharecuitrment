package com.maharecruitment.gov.in.master.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.master.dto.ManpowerDesignationRateRequest;
import com.maharecruitment.gov.in.master.dto.ManpowerDesignationRateResponse;

public interface ManpowerDesignationRateService {

    ManpowerDesignationRateResponse create(ManpowerDesignationRateRequest request);

    ManpowerDesignationRateResponse update(Long rateId, ManpowerDesignationRateRequest request);

    ManpowerDesignationRateResponse getById(Long rateId, boolean includeInactive);

    Page<ManpowerDesignationRateResponse> getAll(Long designationId, boolean includeInactive, Pageable pageable);

    void softDelete(Long rateId);

    void restore(Long rateId);
}

