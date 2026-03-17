package com.maharecruitment.gov.in.master.service;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.master.dto.DepartmentRequest;
import com.maharecruitment.gov.in.master.dto.DepartmentResponse;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;

public interface DepartmentMstService {

    DepartmentResponse create(DepartmentRequest request);

    DepartmentResponse update(Long departmentId, DepartmentRequest request);

    DepartmentResponse getById(Long departmentId);

    Page<DepartmentResponse> getAll(Pageable pageable);

    void delete(Long departmentId);

	public List<DepartmentMst> getAllDepartment();
}
