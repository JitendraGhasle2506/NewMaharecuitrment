package com.maharecruitment.gov.in.asset.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.asset.entity.AssetAllocationEntity;
import com.maharecruitment.gov.in.asset.entity.AssetAuditLogEntity;
import com.maharecruitment.gov.in.asset.entity.AssetCategoryEntity;
import com.maharecruitment.gov.in.asset.entity.AssetEntity;
import com.maharecruitment.gov.in.asset.entity.AssetOwnershipType;
import com.maharecruitment.gov.in.asset.repository.AssetAllocationRepository;
import com.maharecruitment.gov.in.asset.repository.AssetAuditLogRepository;
import com.maharecruitment.gov.in.asset.repository.AssetCategoryRepository;
import com.maharecruitment.gov.in.asset.repository.AssetRepository;
import com.maharecruitment.gov.in.asset.service.AssetService;
import com.maharecruitment.gov.in.asset.service.model.AssetAllocationListView;
import com.maharecruitment.gov.in.asset.service.model.AssetDashboardSummary;
import com.maharecruitment.gov.in.asset.service.model.AssetListView;
import com.maharecruitment.gov.in.asset.service.model.AssetOptionView;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

@Service
@Transactional
public class AssetServiceImpl implements AssetService {

    private final AssetCategoryRepository categoryRepository;
    private final AssetRepository assetRepository;
    private final AssetAllocationRepository allocationRepository;
    private final EmployeeRepository employeeRepository;
    private final AssetAuditLogRepository auditLogRepository;

    public AssetServiceImpl(AssetCategoryRepository categoryRepository, AssetRepository assetRepository,
            AssetAllocationRepository allocationRepository, EmployeeRepository employeeRepository,
            AssetAuditLogRepository auditLogRepository) {
        this.categoryRepository = categoryRepository;
        this.assetRepository = assetRepository;
        this.allocationRepository = allocationRepository;
        this.employeeRepository = employeeRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetCategoryEntity> getAllActiveCategories() {
        return categoryRepository.findByIsActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public AssetDashboardSummary getDashboardSummary() {
        return assetRepository.getDashboardSummary();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetListView> getAssets(Pageable pageable) {
        return assetRepository.findAll(pageable).map(this::toAssetListView);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetAllocationListView> getActiveAllocations(String searchTerm, Pageable pageable) {
        String normalizedSearch = trimToNull(searchTerm);
        Page<AssetAllocationEntity> allocations = normalizedSearch == null
                ? allocationRepository.findByAllocationStatus("ASSIGNED", pageable)
                : allocationRepository.searchActiveAllocations(
                        normalizedSearch.toLowerCase(Locale.ROOT), pageable);
        return allocations.map(this::toAllocationListView);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetOptionView> getAvailableAssetOptions() {
        return assetRepository.findAvailableOptions();
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
    public AssetEntity createAsset(AssetEntity asset, String actorUsername, String ipAddress) {
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
        validateAndNormalizeOwnership(asset);
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
        AssetEntity savedAsset = assetRepository.save(asset);
        saveAuditLog(savedAsset, "CREATED", actorUsername, ipAddress,
                "Asset created. " + describeOwnership(savedAsset));
        return savedAsset;
    }

    @Override
    public AssetAllocationEntity allocateAsset(Long assetId, Long employeeId, String remarks,
            String actorUsername, String ipAddress) {
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

        AssetAllocationEntity savedAllocation = allocationRepository.save(allocation);
        saveAuditLog(asset, "ALLOCATED", actorUsername, ipAddress,
                "Allocated to employee " + employee.getEmployeeCode()
                        + optionalRemarks(allocation.getRemarks()));
        return savedAllocation;
    }

    @Override
    public AssetAllocationEntity replaceAsset(Long allocationId, Long replacementAssetId,
            String replacementReason, String actorUsername, String ipAddress) {
        if (allocationId == null || replacementAssetId == null) {
            throw new IllegalArgumentException("Current allocation and replacement asset are required.");
        }
        String reason = requiredText(replacementReason, "Replacement reason is required.");
        if (reason.length() > 1000) {
            throw new IllegalArgumentException("Replacement reason must not exceed 1000 characters.");
        }
        AssetAllocationEntity currentAllocation = allocationRepository.findByIdForUpdate(allocationId)
                .orElseThrow(() -> new IllegalArgumentException("Active asset allocation not found."));
        if (!"ASSIGNED".equals(currentAllocation.getAllocationStatus())) {
            throw new IllegalStateException("Only an active allocation can be replaced.");
        }

        Long currentAssetId = currentAllocation.getAsset().getId();
        if (currentAssetId.equals(replacementAssetId)) {
            throw new IllegalArgumentException("Select a different replacement asset.");
        }
        AssetEntity[] lockedAssets = lockAssetsInIdOrder(currentAssetId, replacementAssetId);
        AssetEntity currentAsset = lockedAssets[0].getId().equals(currentAssetId)
                ? lockedAssets[0]
                : lockedAssets[1];
        AssetEntity replacementAsset = lockedAssets[0].getId().equals(replacementAssetId)
                ? lockedAssets[0]
                : lockedAssets[1];

        if (!"ALLOCATED".equals(currentAsset.getStatus())) {
            throw new IllegalStateException("The current asset is not marked as allocated.");
        }
        if (!"AVAILABLE".equals(replacementAsset.getStatus())) {
            throw new IllegalStateException("The selected replacement asset is no longer available.");
        }

        currentAllocation.setAllocationStatus("REPLACED");
        currentAllocation.setReturnedDate(LocalDate.now());
        currentAllocation.setRemarks(appendRemarks(
                currentAllocation.getRemarks(), "Replacement reason: " + reason));
        currentAsset.setStatus("AVAILABLE");
        replacementAsset.setStatus("ALLOCATED");

        AssetAllocationEntity replacementAllocation = new AssetAllocationEntity();
        replacementAllocation.setAsset(replacementAsset);
        replacementAllocation.setEmployee(currentAllocation.getEmployee());
        replacementAllocation.setReplacesAllocation(currentAllocation);
        replacementAllocation.setAllocatedDate(LocalDate.now());
        replacementAllocation.setAllocationStatus("ASSIGNED");
        replacementAllocation.setRemarks(limit(
                "Replaced asset " + currentAsset.getAssetCode() + "; reason: " + reason, 1000));

        assetRepository.saveAll(List.of(currentAsset, replacementAsset));
        allocationRepository.save(currentAllocation);
        AssetAllocationEntity savedReplacement = allocationRepository.save(replacementAllocation);

        saveAuditLog(currentAsset, "REPLACED_OUT", actorUsername, ipAddress,
                limit("Old asset: " + currentAsset.getAssetName() + " (" + currentAsset.getAssetCode()
                        + "), ownership: " + currentAsset.getOwnershipType() + "; replaced by asset "
                        + replacementAsset.getAssetCode() + "; reason: " + reason, 2000));
        saveAuditLog(replacementAsset, "REPLACED_IN", actorUsername, ipAddress,
                limit("Replaced old asset " + currentAsset.getAssetName() + " (" + currentAsset.getAssetCode()
                        + "), ownership: " + currentAsset.getOwnershipType() + " for employee "
                        + currentAllocation.getEmployee().getEmployeeCode() + "; reason: " + reason, 2000));
        return savedReplacement;
    }

    @Override
    public void returnAsset(Long allocationId, String remarks, String actorUsername, String ipAddress) {
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
        saveAuditLog(asset, "RETURNED", actorUsername, ipAddress,
                "Returned by employee " + allocation.getEmployee().getEmployeeCode()
                        + optionalRemarks(returnRemarks));
    }

    private void validateAndNormalizeOwnership(AssetEntity asset) {
        AssetOwnershipType ownershipType = asset.getOwnershipType();
        if (ownershipType == null) {
            throw new IllegalArgumentException("Ownership type is required.");
        }
        if (ownershipType == AssetOwnershipType.OWNED) {
            asset.setRentStartDate(null);
            asset.setRentEndDate(null);
            return;
        }
        if (asset.getRentStartDate() == null) {
            throw new IllegalArgumentException("Rent start date is required for a rented asset.");
        }
        if (asset.getRentEndDate() != null && asset.getRentEndDate().isBefore(asset.getRentStartDate())) {
            throw new IllegalArgumentException("Rent end date cannot be before the rent start date.");
        }
        asset.setPurchaseDate(null);
        asset.setWarrantyExpiryDate(null);
    }

    private AssetListView toAssetListView(AssetEntity asset) {
        return new AssetListView(
                asset.getId(),
                asset.getAssetCode(),
                asset.getAssetName(),
                asset.getAssetCategory().getCategoryName(),
                asset.getOwnershipType(),
                asset.getRentStartDate(),
                asset.getRentEndDate(),
                asset.getPurchaseDate(),
                asset.getWarrantyExpiryDate(),
                asset.getStatus());
    }

    private AssetAllocationListView toAllocationListView(AssetAllocationEntity allocation) {
        return new AssetAllocationListView(
                allocation.getId(),
                allocation.getAsset().getAssetName(),
                allocation.getAsset().getAssetCode(),
                allocation.getAsset().getAssetCategory().getCategoryName(),
                allocation.getAsset().getOwnershipType(),
                allocation.getEmployee().getFullName(),
                allocation.getEmployee().getEmployeeCode(),
                allocation.getAllocatedDate(),
                allocation.getAllocationStatus());
    }

    private AssetEntity[] lockAssetsInIdOrder(Long currentAssetId, Long replacementAssetId) {
        Long firstId = Math.min(currentAssetId, replacementAssetId);
        Long secondId = Math.max(currentAssetId, replacementAssetId);
        AssetEntity first = assetRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found."));
        AssetEntity second = assetRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found."));
        return new AssetEntity[] { first, second };
    }

    private String appendRemarks(String existingRemarks, String additionalRemarks) {
        String existing = trimToNull(existingRemarks);
        return limit(existing == null ? additionalRemarks : existing + " | " + additionalRemarks, 1000);
    }

    private void saveAuditLog(AssetEntity asset, String actionType, String actorUsername,
            String ipAddress, String details) {
        AssetAuditLogEntity auditLog = new AssetAuditLogEntity();
        auditLog.setAsset(asset);
        auditLog.setActionType(actionType);
        auditLog.setActorUsername(normalizeAuditValue(actorUsername, "anonymous", 150));
        auditLog.setIpAddress(normalizeAuditValue(ipAddress, "unknown", 64));
        auditLog.setDetails(limit(details, 2000));
        auditLogRepository.save(auditLog);
    }

    private String describeOwnership(AssetEntity asset) {
        if (asset.getOwnershipType() == AssetOwnershipType.OWNED) {
            return "Ownership: OWNED.";
        }
        String endDate = asset.getRentEndDate() == null ? "open-ended" : asset.getRentEndDate().toString();
        return "Ownership: RENTED; rent period: " + asset.getRentStartDate() + " to " + endDate + ".";
    }

    private String optionalRemarks(String remarks) {
        return remarks == null ? "." : "; remarks: " + remarks + ".";
    }

    private String normalizeAuditValue(String value, String fallback, int maxLength) {
        String normalized = trimToNull(value);
        return limit(normalized == null ? fallback : normalized, maxLength);
    }

    private String limit(String value, int maxLength) {
        return value != null && value.length() > maxLength ? value.substring(0, maxLength) : value;
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
