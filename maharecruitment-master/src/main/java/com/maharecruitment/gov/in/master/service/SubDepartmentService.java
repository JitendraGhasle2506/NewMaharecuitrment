package com.maharecruitment.gov.in.master.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.master.dto.SubDepartmentRequest;
import com.maharecruitment.gov.in.master.dto.SubDepartmentResponse;

public interface SubDepartmentService {

    SubDepartmentResponse create(SubDepartmentRequest request);

    SubDepartmentResponse update(Long subDeptId, SubDepartmentRequest request);

    SubDepartmentResponse getById(Long subDeptId);

    Page<SubDepartmentResponse> getAll(Long departmentId, Pageable pageable);

    void delete(Long subDeptId);
}
