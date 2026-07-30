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
    public List<AssetCategoryEntity> getAllActiveCategories() {
        return categoryRepository.findByIsActiveTrue();
    }

    @Override
    public List<AssetEntity> getAllAssets() {
        return assetRepository.findAll();
    }

    @Override
    public List<AssetAllocationEntity> getAssetAllocations() {
        return allocationRepository.findAll();
    }

    @Override
    public AssetCategoryEntity createCategory(AssetCategoryEntity category) {
        return categoryRepository.save(category);
    }

    @Override
    public AssetEntity createAsset(AssetEntity asset) {
        asset.setStatus("AVAILABLE");
        return assetRepository.save(asset);
    }

    @Override
    public AssetAllocationEntity allocateAsset(Long assetId, Long employeeId, String remarks) {
        AssetEntity asset = assetRepository.findById(assetId).orElseThrow(() -> new RuntimeException("Asset not found"));
        if (!"AVAILABLE".equals(asset.getStatus())) {
            throw new RuntimeException("Asset is not available for allocation");
        }

        EmployeeEntity employee = employeeRepository.findById(employeeId).orElseThrow(() -> new RuntimeException("Employee not found"));

        AssetAllocationEntity allocation = new AssetAllocationEntity();
        allocation.setAsset(asset);
        allocation.setEmployee(employee);
        allocation.setAllocatedDate(LocalDate.now());
        allocation.setAllocationStatus("ASSIGNED");
        allocation.setRemarks(remarks);

        asset.setStatus("ALLOCATED");
        assetRepository.save(asset);

        return allocationRepository.save(allocation);
    }

    @Override
    public void returnAsset(Long allocationId, String remarks) {
        AssetAllocationEntity allocation = allocationRepository.findById(allocationId).orElseThrow(() -> new RuntimeException("Allocation not found"));
        allocation.setAllocationStatus("RETURNED");
        allocation.setReturnedDate(LocalDate.now());
        allocation.setRemarks(allocation.getRemarks() + " | Return remarks: " + remarks);

        AssetEntity asset = allocation.getAsset();
        asset.setStatus("AVAILABLE");

        assetRepository.save(asset);
        allocationRepository.save(allocation);
    }
}
