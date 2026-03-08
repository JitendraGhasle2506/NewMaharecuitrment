package com.maharecruitment.gov.in.master.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.master.dto.SubDepartmentRequest;
import com.maharecruitment.gov.in.master.dto.SubDepartmentResponse;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;
import com.maharecruitment.gov.in.master.entity.SubDepartment;
import com.maharecruitment.gov.in.master.exception.DuplicateResourceException;
import com.maharecruitment.gov.in.master.exception.ResourceNotFoundException;
import com.maharecruitment.gov.in.master.mapper.SubDepartmentMapper;
import com.maharecruitment.gov.in.master.repository.DepartmentMstRepository;
import com.maharecruitment.gov.in.master.repository.SubDepartmentRepository;
import com.maharecruitment.gov.in.master.service.SubDepartmentService;

@Service
@Transactional(readOnly = true)
public class SubDepartmentServiceImpl implements SubDepartmentService {

    private final SubDepartmentRepository subDepartmentRepository;
    private final DepartmentMstRepository departmentRepository;
    private final SubDepartmentMapper mapper;

    public SubDepartmentServiceImpl(
            SubDepartmentRepository subDepartmentRepository,
            DepartmentMstRepository departmentRepository,
            SubDepartmentMapper mapper) {
        this.subDepartmentRepository = subDepartmentRepository;
        this.departmentRepository = departmentRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public SubDepartmentResponse create(SubDepartmentRequest request) {
        DepartmentMst department = findDepartmentById(request.getDepartmentId());
        String subDepartmentName = normalizeName(request.getSubDeptName());
        ensureUniqueSubDepartmentName(request.getDepartmentId(), subDepartmentName, null);

        SubDepartment entity = new SubDepartment();
        entity.setSubDeptName(subDepartmentName);
        entity.setDepartment(department);

        return mapper.toResponse(subDepartmentRepository.save(entity));
    }

    @Override
    @Transactional
    public SubDepartmentResponse update(Long subDeptId, SubDepartmentRequest request) {
        SubDepartment entity = subDepartmentRepository.findById(subDeptId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-department not found for id: " + subDeptId));
        DepartmentMst department = findDepartmentById(request.getDepartmentId());
        String subDepartmentName = normalizeName(request.getSubDeptName());

        ensureUniqueSubDepartmentName(request.getDepartmentId(), subDepartmentName, subDeptId);

        entity.setSubDeptName(subDepartmentName);
        entity.setDepartment(department);

        return mapper.toResponse(subDepartmentRepository.save(entity));
    }

    @Override
    public SubDepartmentResponse getById(Long subDeptId) {
        SubDepartment entity = subDepartmentRepository.findById(subDeptId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-department not found for id: " + subDeptId));
        return mapper.toResponse(entity);
    }

    @Override
    public Page<SubDepartmentResponse> getAll(Long departmentId, Pageable pageable) {
        if (departmentId != null) {
            findDepartmentById(departmentId);
            return subDepartmentRepository.findByDepartmentDepartmentId(departmentId, pageable)
                    .map(mapper::toResponse);
        }

        return subDepartmentRepository.findAll(pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional
    public void delete(Long subDeptId) {
        SubDepartment entity = subDepartmentRepository.findById(subDeptId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-department not found for id: " + subDeptId));
        subDepartmentRepository.delete(entity);
    }

    private DepartmentMst findDepartmentById(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found for id: " + departmentId));
    }

    private void ensureUniqueSubDepartmentName(Long departmentId, String subDeptName, Long excludeId) {
        if (subDepartmentRepository.existsByDepartmentIdAndSubDeptNameExcludingId(departmentId, subDeptName,
                excludeId)) {
            throw new DuplicateResourceException(
                    "Sub-department already exists for departmentId=" + departmentId + ", name=" + subDeptName);
        }
    }

    private String normalizeName(String value) {
        return value == null ? null : value.trim();
    }
}
