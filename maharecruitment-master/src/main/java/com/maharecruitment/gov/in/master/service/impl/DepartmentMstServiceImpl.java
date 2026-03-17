package com.maharecruitment.gov.in.master.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.master.dto.DepartmentRequest;
import com.maharecruitment.gov.in.master.dto.DepartmentResponse;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;
import com.maharecruitment.gov.in.master.exception.BusinessValidationException;
import com.maharecruitment.gov.in.master.exception.DuplicateResourceException;
import com.maharecruitment.gov.in.master.exception.ResourceNotFoundException;
import com.maharecruitment.gov.in.master.mapper.DepartmentMapper;
import com.maharecruitment.gov.in.master.repository.DepartmentMstRepository;
import com.maharecruitment.gov.in.master.repository.SubDepartmentRepository;
import com.maharecruitment.gov.in.master.service.DepartmentMstService;

@Service
@Transactional(readOnly = true)
public class DepartmentMstServiceImpl implements DepartmentMstService {

    private final DepartmentMstRepository departmentRepository;
    private final SubDepartmentRepository subDepartmentRepository;
    private final DepartmentMapper mapper;

    public DepartmentMstServiceImpl(
            DepartmentMstRepository departmentRepository,
            SubDepartmentRepository subDepartmentRepository,
            DepartmentMapper mapper) {
        this.departmentRepository = departmentRepository;
        this.subDepartmentRepository = subDepartmentRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        String departmentName = normalizeName(request.getDepartmentName());
        ensureUniqueDepartmentName(departmentName, null);

        DepartmentMst entity = new DepartmentMst();
        entity.setDepartmentName(departmentName);

        return mapper.toResponse(departmentRepository.save(entity), false);
    }

    @Override
    @Transactional
    public DepartmentResponse update(Long departmentId, DepartmentRequest request) {
        DepartmentMst entity = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found for id: " + departmentId));

        String departmentName = normalizeName(request.getDepartmentName());
        ensureUniqueDepartmentName(departmentName, departmentId);
        entity.setDepartmentName(departmentName);

        return mapper.toResponse(departmentRepository.save(entity), false);
    }

    @Override
    public DepartmentResponse getById(Long departmentId) {
        DepartmentMst entity = departmentRepository.findDetailedByDepartmentId(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found for id: " + departmentId));
        return mapper.toResponse(entity, true);
    }

    @Override
    public Page<DepartmentResponse> getAll(Pageable pageable) {
        return departmentRepository.findAll(pageable).map(entity -> mapper.toResponse(entity, false));
    }

    @Override
    @Transactional
    public void delete(Long departmentId) {
        DepartmentMst entity = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found for id: " + departmentId));

        if (subDepartmentRepository.existsByDepartmentDepartmentId(departmentId)) {
            throw new BusinessValidationException(
                    "Cannot delete department because sub-departments exist. Delete sub-departments first.");
        }

        departmentRepository.delete(entity);
    }

    private void ensureUniqueDepartmentName(String departmentName, Long excludeId) {
        if (departmentRepository.existsByDepartmentNameExcludingId(departmentName, excludeId)) {
            throw new DuplicateResourceException("Department already exists with name: " + departmentName);
        }
    }

    private String normalizeName(String value) {
        return value == null ? null : value.trim();
    }

	@Override
	public List<DepartmentMst> getAllDepartment() {
		// TODO Auto-generated method stub
		return null;
	}
}
