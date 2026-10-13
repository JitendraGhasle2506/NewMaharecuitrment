package com.maharecruitment.gov.in.asset.service;

import com.maharecruitment.gov.in.asset.entity.AssetCategoryEntity;
import com.maharecruitment.gov.in.asset.entity.AssetEntity;
import com.maharecruitment.gov.in.asset.entity.AssetAllocationEntity;
import com.maharecruitment.gov.in.asset.service.model.AssetAllocationListView;
import com.maharecruitment.gov.in.asset.service.model.AssetDashboardSummary;
import com.maharecruitment.gov.in.asset.service.model.AssetListView;
import com.maharecruitment.gov.in.asset.service.model.AssetOptionView;

import java.util.List;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssetService {
    List<AssetCategoryEntity> getAllActiveCategories();
    AssetDashboardSummary getDashboardSummary();
    Page<AssetListView> getAssets(Pageable pageable);
    Page<AssetAllocationListView> getActiveAllocations(String searchTerm, Pageable pageable);
    List<AssetOptionView> getAvailableAssetOptions();
    AssetCategoryEntity createCategory(AssetCategoryEntity category);
    AssetEntity createAsset(AssetEntity asset, String actorUsername, String ipAddress);
    AssetAllocationEntity allocateAsset(Long assetId, Long employeeId, LocalDate allocatedDate, String remarks,
            String actorUsername, String ipAddress);
    AssetAllocationEntity replaceAsset(Long allocationId, Long replacementAssetId, String replacementReason,
            String actorUsername, String ipAddress);
    void returnAsset(Long allocationId, String remarks, String actorUsername, String ipAddress);
}
