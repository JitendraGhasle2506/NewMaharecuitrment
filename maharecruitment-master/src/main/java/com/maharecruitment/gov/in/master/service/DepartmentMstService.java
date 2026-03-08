package com.maharecruitment.gov.in.master.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.master.dto.DepartmentRequest;
import com.maharecruitment.gov.in.master.dto.DepartmentResponse;

public interface DepartmentMstService {

    DepartmentResponse create(DepartmentRequest request);

    DepartmentResponse update(Long departmentId, DepartmentRequest request);

    DepartmentResponse getById(Long departmentId);

    Page<DepartmentResponse> getAll(Pageable pageable);

    void delete(Long departmentId);
}
