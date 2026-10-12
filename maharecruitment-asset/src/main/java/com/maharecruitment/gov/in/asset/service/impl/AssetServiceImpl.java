package com.maharecruitment.gov.in.asset.service.impl;

import com.maharecruitment.gov.in.asset.entity.AssetCategoryEntity;
import com.maharecruitment.gov.in.asset.entity.AssetEntity;
import com.maharecruitment.gov.in.asset.entity.AssetAllocationEntity;
import com.maharecruitment.gov.in.asset.repository.AssetCategoryRepository;
import com.maharecruitment.gov.in.asset.repository.AssetRepository;
import com.maharecruitment.gov.in.asset.repository.AssetAllocationRepository;
import com.maharecruitment.gov.in.asset.service.AssetService;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class AssetServiceImpl implements AssetService {

    private final AssetCategoryRepository categoryRepository;
    private final AssetRepository assetRepository;
    private final AssetAllocationRepository allocationRepository;
    private final EmployeeRepository employeeRepository;

    public AssetServiceImpl(AssetCategoryRepository categoryRepository, AssetRepository assetRepository, AssetAllocationRepository allocationRepository, EmployeeRepository employeeRepository) {
        this.categoryRepository = categoryRepository;
        this.assetRepository = assetRepository;
        this.allocationRepository = allocationRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetCategoryEntity> getAllActiveCategories() {
        return categoryRepository.findByIsActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetEntity> getAllAssets() {
        return assetRepository.findAllByOrderByAssetCodeAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetAllocationEntity> getAssetAllocations() {
        return allocationRepository.findByAllocationStatusOrderByAllocatedDateDescIdDesc("ASSIGNED");
    }

    @Override
    public AssetCategoryEntity createCategory(AssetCategoryEntity category) {
        if (category == null || category.getCategoryName() == null || category.getCategoryName().isBlank()) {
            throw new IllegalArgumentException("Category name is required.");
        }
        String categoryName = category.getCategoryName().trim();
        if (categoryRepository.existsByCategoryNameIgnoreCase(categoryName)) {
            throw new IllegalArgumentException("An asset category with this name already exists.");
        }
        category.setId(null);
        category.setCategoryName(categoryName);
        category.setDescription(trimToNull(category.getDescription()));
        category.setIsActive(Boolean.TRUE);
        return categoryRepository.save(category);
    }

    @Override
    public AssetEntity createAsset(AssetEntity asset) {
        if (asset == null || asset.getAssetCategory() == null || asset.getAssetCategory().getId() == null) {
            throw new IllegalArgumentException("Asset category is required.");
        }
        String assetCode = requiredText(asset.getAssetCode(), "Asset code is required.");
        String assetName = requiredText(asset.getAssetName(), "Asset name is required.");
        if (assetRepository.existsByAssetCodeIgnoreCase(assetCode)) {
            throw new IllegalArgumentException("An asset with this code already exists.");
        }
        AssetCategoryEntity category = categoryRepository.findById(asset.getAssetCategory().getId())
                .filter(item -> Boolean.TRUE.equals(item.getIsActive()))
                .orElseThrow(() -> new IllegalArgumentException("Select a valid active asset category."));
        if (asset.getPurchaseDate() != null && asset.getWarrantyExpiryDate() != null
                && asset.getWarrantyExpiryDate().isBefore(asset.getPurchaseDate())) {
            throw new IllegalArgumentException("Warranty expiry date cannot be before the purchase date.");
        }
        asset.setId(null);
        asset.setAssetCategory(category);
        asset.setAssetCode(assetCode);
        asset.setAssetName(assetName);
        asset.setNotes(trimToNull(asset.getNotes()));
        asset.setStatus("AVAILABLE");
        return assetRepository.save(asset);
    }

    @Override
    public AssetAllocationEntity allocateAsset(Long assetId, Long employeeId, String remarks) {
        if (assetId == null || employeeId == null) {
            throw new IllegalArgumentException("Asset and employee are required.");
        }
        AssetEntity asset = assetRepository.findByIdForUpdate(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found."));
        if (!"AVAILABLE".equals(asset.getStatus())) {
            throw new IllegalStateException("Asset is not available for allocation.");
        }

        EmployeeEntity employee = employeeRepository.findById(employeeId)
                .filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("Select a valid active employee."));

        AssetAllocationEntity allocation = new AssetAllocationEntity();
        allocation.setAsset(asset);
        allocation.setEmployee(employee);
        allocation.setAllocatedDate(LocalDate.now());
        allocation.setAllocationStatus("ASSIGNED");
        allocation.setRemarks(trimToNull(remarks));

        asset.setStatus("ALLOCATED");
        assetRepository.save(asset);

        return allocationRepository.save(allocation);
    }

    @Override
    public void returnAsset(Long allocationId, String remarks) {
        if (allocationId == null) {
            throw new IllegalArgumentException("Allocation is required.");
        }
        AssetAllocationEntity allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new IllegalArgumentException("Allocation not found."));
        if (!"ASSIGNED".equals(allocation.getAllocationStatus())) {
            throw new IllegalStateException("This asset allocation has already been returned.");
        }
        allocation.setAllocationStatus("RETURNED");
        allocation.setReturnedDate(LocalDate.now());
        String returnRemarks = trimToNull(remarks);
        String existingRemarks = trimToNull(allocation.getRemarks());
        allocation.setRemarks(existingRemarks == null
                ? "Return remarks: " + (returnRemarks == null ? "None" : returnRemarks)
                : existingRemarks + " | Return remarks: " + (returnRemarks == null ? "None" : returnRemarks));

        AssetEntity asset = allocation.getAsset();
        asset.setStatus("AVAILABLE");

        assetRepository.save(asset);
        allocationRepository.save(allocation);
    }

    private String requiredText(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
