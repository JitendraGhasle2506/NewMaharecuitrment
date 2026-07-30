package com.maharecruitment.gov.in.master.service;

import java.util.List;

import com.maharecruitment.gov.in.master.dto.DesignationCategoryMasterRequest;
import com.maharecruitment.gov.in.master.dto.DesignationCategoryMasterResponse;

public interface DesignationCategoryMasterService {

    DesignationCategoryMasterResponse create(DesignationCategoryMasterRequest request);

    DesignationCategoryMasterResponse update(Long categoryId, DesignationCategoryMasterRequest request);

    DesignationCategoryMasterResponse getById(Long categoryId);

    List<DesignationCategoryMasterResponse> getAll(boolean includeInactive);

    void softDelete(Long categoryId);

    void restore(Long categoryId);
}
