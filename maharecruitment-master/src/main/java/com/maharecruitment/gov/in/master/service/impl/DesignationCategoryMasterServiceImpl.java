package com.maharecruitment.gov.in.master.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.master.dto.DesignationCategoryMasterRequest;
import com.maharecruitment.gov.in.master.dto.DesignationCategoryMasterResponse;
import com.maharecruitment.gov.in.master.entity.DesignationCategoryMaster;
import com.maharecruitment.gov.in.master.exception.DuplicateResourceException;
import com.maharecruitment.gov.in.master.exception.ResourceNotFoundException;
import com.maharecruitment.gov.in.master.mapper.DesignationCategoryMasterMapper;
import com.maharecruitment.gov.in.master.repository.DesignationCategoryMasterRepository;
import com.maharecruitment.gov.in.master.service.DesignationCategoryMasterService;

@Service
@Transactional(readOnly = true)
public class DesignationCategoryMasterServiceImpl implements DesignationCategoryMasterService {

    private final DesignationCategoryMasterRepository repository;
    private final DesignationCategoryMasterMapper mapper;

    public DesignationCategoryMasterServiceImpl(DesignationCategoryMasterRepository repository,
            DesignationCategoryMasterMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public DesignationCategoryMasterResponse create(DesignationCategoryMasterRequest request) {
        if (repository.existsByCategoryNameIgnoreCase(request.getCategoryName())) {
            throw new DuplicateResourceException("Category already exists: " + request.getCategoryName());
        }
        DesignationCategoryMaster entity = DesignationCategoryMaster.builder()
                .categoryName(request.getCategoryName())
                .activeFlag(request.getActiveFlag() != null ? request.getActiveFlag() : "Y")
                .build();
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public DesignationCategoryMasterResponse update(Long categoryId, DesignationCategoryMasterRequest request) {
        DesignationCategoryMaster entity = repository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
        
        repository.findByCategoryNameIgnoreCase(request.getCategoryName())
                .ifPresent(existing -> {
                    if (!existing.getCategoryId().equals(categoryId)) {
                        throw new DuplicateResourceException("Category already exists: " + request.getCategoryName());
                    }
                });

        entity.setCategoryName(request.getCategoryName());
        if (request.getActiveFlag() != null) {
            entity.setActiveFlag(request.getActiveFlag());
        }
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public DesignationCategoryMasterResponse getById(Long categoryId) {
        return repository.findById(categoryId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
    }

    @Override
    public List<DesignationCategoryMasterResponse> getAll(boolean includeInactive) {
        List<DesignationCategoryMaster> categories = includeInactive
                ? repository.findAll()
                : repository.findByActiveFlag("Y");
        return categories.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void softDelete(Long categoryId) {
        DesignationCategoryMaster entity = repository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
        entity.setActiveFlag("N");
        repository.save(entity);
    }

    @Override
    @Transactional
    public void restore(Long categoryId) {
        DesignationCategoryMaster entity = repository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
        entity.setActiveFlag("Y");
        repository.save(entity);
    }
}
