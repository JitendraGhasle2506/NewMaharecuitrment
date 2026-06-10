package com.maharecruitment.gov.in.asset.service;

import com.maharecruitment.gov.in.asset.entity.AssetCategoryEntity;
import com.maharecruitment.gov.in.asset.entity.AssetEntity;
import com.maharecruitment.gov.in.asset.entity.AssetAllocationEntity;

import java.util.List;

public interface AssetService {
    List<AssetCategoryEntity> getAllActiveCategories();
    List<AssetEntity> getAllAssets();
    List<AssetAllocationEntity> getAssetAllocations();
    AssetCategoryEntity createCategory(AssetCategoryEntity category);
    AssetEntity createAsset(AssetEntity asset);
    AssetAllocationEntity allocateAsset(Long assetId, Long employeeId, String remarks);
    void returnAsset(Long allocationId, String remarks);
}
