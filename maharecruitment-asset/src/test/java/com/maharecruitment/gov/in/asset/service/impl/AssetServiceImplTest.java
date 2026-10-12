package com.maharecruitment.gov.in.asset.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.maharecruitment.gov.in.asset.entity.AssetAllocationEntity;
import com.maharecruitment.gov.in.asset.entity.AssetCategoryEntity;
import com.maharecruitment.gov.in.asset.entity.AssetEntity;
import com.maharecruitment.gov.in.asset.repository.AssetAllocationRepository;
import com.maharecruitment.gov.in.asset.repository.AssetCategoryRepository;
import com.maharecruitment.gov.in.asset.repository.AssetRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

class AssetServiceImplTest {

    private final AssetCategoryRepository categoryRepository = mock(AssetCategoryRepository.class);
    private final AssetRepository assetRepository = mock(AssetRepository.class);
    private final AssetAllocationRepository allocationRepository = mock(AssetAllocationRepository.class);
    private final EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
    private AssetServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AssetServiceImpl(
                categoryRepository,
                assetRepository,
                allocationRepository,
                employeeRepository);
    }

    @Test
    void loadsAssetsAndActiveAllocationsUsingFetchOptimizedQueries() {
        when(assetRepository.findAllByOrderByAssetCodeAsc()).thenReturn(List.of());
        when(allocationRepository.findByAllocationStatusOrderByAllocatedDateDescIdDesc("ASSIGNED"))
                .thenReturn(List.of());

        assertThat(service.getAllAssets()).isEmpty();
        assertThat(service.getAssetAllocations()).isEmpty();

        verify(assetRepository).findAllByOrderByAssetCodeAsc();
        verify(allocationRepository)
                .findByAllocationStatusOrderByAllocatedDateDescIdDesc("ASSIGNED");
    }

    @Test
    void createsAssetWithManagedCategoryAndNormalizedFields() {
        AssetCategoryEntity selectedCategory = new AssetCategoryEntity();
        selectedCategory.setId(4L);
        selectedCategory.setIsActive(true);
        AssetEntity asset = new AssetEntity();
        AssetCategoryEntity submittedCategory = new AssetCategoryEntity();
        submittedCategory.setId(4L);
        asset.setAssetCategory(submittedCategory);
        asset.setAssetCode("  LAP-100  ");
        asset.setAssetName("  Finance Laptop  ");
        asset.setNotes("  New device  ");
        when(categoryRepository.findById(4L)).thenReturn(Optional.of(selectedCategory));
        when(assetRepository.save(asset)).thenReturn(asset);

        AssetEntity saved = service.createAsset(asset);

        assertThat(saved.getAssetCategory()).isSameAs(selectedCategory);
        assertThat(saved.getAssetCode()).isEqualTo("LAP-100");
        assertThat(saved.getAssetName()).isEqualTo("Finance Laptop");
        assertThat(saved.getNotes()).isEqualTo("New device");
        assertThat(saved.getStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    void rejectsInvalidAssetDatesAndDuplicateCodes() {
        AssetCategoryEntity category = new AssetCategoryEntity();
        category.setId(2L);
        category.setIsActive(true);
        AssetEntity asset = new AssetEntity();
        asset.setAssetCategory(category);
        asset.setAssetCode("LAP-200");
        asset.setAssetName("Laptop");
        asset.setPurchaseDate(LocalDate.of(2026, 10, 9));
        asset.setWarrantyExpiryDate(LocalDate.of(2026, 10, 8));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.createAsset(asset))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Warranty expiry date cannot be before the purchase date.");

        asset.setWarrantyExpiryDate(LocalDate.of(2027, 10, 9));
        when(assetRepository.existsByAssetCodeIgnoreCase("LAP-200")).thenReturn(true);
        assertThatThrownBy(() -> service.createAsset(asset))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("An asset with this code already exists.");
    }

    @Test
    void allocatesOnlyAnAvailableAssetToAnActiveEmployee() {
        AssetEntity asset = new AssetEntity();
        asset.setId(8L);
        asset.setStatus("AVAILABLE");
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(12L);
        employee.setStatus("ACTIVE");
        when(assetRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(asset));
        when(employeeRepository.findById(12L)).thenReturn(Optional.of(employee));
        when(allocationRepository.save(org.mockito.ArgumentMatchers.any(AssetAllocationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AssetAllocationEntity allocation = service.allocateAsset(8L, 12L, " New laptop ");

        assertThat(asset.getStatus()).isEqualTo("ALLOCATED");
        assertThat(allocation.getEmployee()).isSameAs(employee);
        assertThat(allocation.getAllocationStatus()).isEqualTo("ASSIGNED");
        assertThat(allocation.getRemarks()).isEqualTo("New laptop");
    }

    @Test
    void returnsAssetWithoutWritingNullIntoRemarks() {
        AssetEntity asset = new AssetEntity();
        asset.setStatus("ALLOCATED");
        AssetAllocationEntity allocation = new AssetAllocationEntity();
        allocation.setId(18L);
        allocation.setAsset(asset);
        allocation.setAllocationStatus("ASSIGNED");
        when(allocationRepository.findById(18L)).thenReturn(Optional.of(allocation));

        service.returnAsset(18L, " Working condition ");

        assertThat(asset.getStatus()).isEqualTo("AVAILABLE");
        assertThat(allocation.getAllocationStatus()).isEqualTo("RETURNED");
        assertThat(allocation.getRemarks()).isEqualTo("Return remarks: Working condition");
    }
}
