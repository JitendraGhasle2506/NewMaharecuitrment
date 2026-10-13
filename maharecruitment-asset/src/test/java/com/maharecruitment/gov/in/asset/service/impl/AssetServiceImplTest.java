package com.maharecruitment.gov.in.asset.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.maharecruitment.gov.in.asset.entity.AssetAllocationEntity;
import com.maharecruitment.gov.in.asset.entity.AssetAuditLogEntity;
import com.maharecruitment.gov.in.asset.entity.AssetCategoryEntity;
import com.maharecruitment.gov.in.asset.entity.AssetEntity;
import com.maharecruitment.gov.in.asset.entity.AssetOwnershipType;
import com.maharecruitment.gov.in.asset.repository.AssetAllocationRepository;
import com.maharecruitment.gov.in.asset.repository.AssetAuditLogRepository;
import com.maharecruitment.gov.in.asset.repository.AssetCategoryRepository;
import com.maharecruitment.gov.in.asset.repository.AssetRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import org.mockito.ArgumentCaptor;

class AssetServiceImplTest {

    private final AssetCategoryRepository categoryRepository = mock(AssetCategoryRepository.class);
    private final AssetRepository assetRepository = mock(AssetRepository.class);
    private final AssetAllocationRepository allocationRepository = mock(AssetAllocationRepository.class);
    private final EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
    private final AssetAuditLogRepository auditLogRepository = mock(AssetAuditLogRepository.class);
    private AssetServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AssetServiceImpl(
                categoryRepository,
                assetRepository,
                allocationRepository,
                employeeRepository,
                auditLogRepository);
    }

    @Test
    void loadsAssetsAndActiveAllocationsUsingFetchOptimizedQueries() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(assetRepository.findAll(pageable)).thenReturn(Page.empty(pageable));
        when(allocationRepository.findByAllocationStatus("ASSIGNED", pageable))
                .thenReturn(Page.empty(pageable));

        assertThat(service.getAssets(pageable)).isEmpty();
        assertThat(service.getActiveAllocations("  ", pageable)).isEmpty();

        verify(assetRepository).findAll(pageable);
        verify(allocationRepository).findByAllocationStatus("ASSIGNED", pageable);
    }

    @Test
    void searchesActiveAllocationsUsingANormalizedTerm() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(allocationRepository.searchActiveAllocations("finance", pageable))
                .thenReturn(Page.empty(pageable));

        assertThat(service.getActiveAllocations("  FiNaNcE  ", pageable)).isEmpty();

        verify(allocationRepository).searchActiveAllocations("finance", pageable);
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
        asset.setMakeModel("  Dell Latitude 5420  ");
        asset.setNotes("  New device  ");
        when(categoryRepository.findById(4L)).thenReturn(Optional.of(selectedCategory));
        when(assetRepository.save(asset)).thenReturn(asset);

        AssetEntity saved = service.createAsset(asset, "infra.admin", "192.0.2.10");

        assertThat(saved.getAssetCategory()).isSameAs(selectedCategory);
        assertThat(saved.getAssetCode()).isEqualTo("LAP-100");
        assertThat(saved.getAssetName()).isEqualTo("Finance Laptop");
        assertThat(saved.getMakeModel()).isEqualTo("Dell Latitude 5420");
        assertThat(saved.getNotes()).isEqualTo("New device");
        assertThat(saved.getStatus()).isEqualTo("AVAILABLE");
        assertThat(saved.getOwnershipType()).isEqualTo(AssetOwnershipType.OWNED);

        ArgumentCaptor<AssetAuditLogEntity> auditCaptor = ArgumentCaptor.forClass(AssetAuditLogEntity.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getActionType()).isEqualTo("CREATED");
        assertThat(auditCaptor.getValue().getActorUsername()).isEqualTo("infra.admin");
        assertThat(auditCaptor.getValue().getIpAddress()).isEqualTo("192.0.2.10");
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
        asset.setMakeModel("HP EliteBook 840");
        asset.setPurchaseDate(LocalDate.of(2026, 10, 9));
        asset.setWarrantyExpiryDate(LocalDate.of(2026, 10, 8));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.createAsset(asset, "admin", "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Warranty expiry date cannot be before the purchase date.");

        asset.setWarrantyExpiryDate(LocalDate.of(2027, 10, 9));
        when(assetRepository.existsByAssetCodeIgnoreCase("LAP-200")).thenReturn(true);
        assertThatThrownBy(() -> service.createAsset(asset, "admin", "127.0.0.1"))
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

        employee.setEmployeeCode("EMP-12");
        LocalDate selectedAllocationDate = LocalDate.of(2026, 10, 10);
        AssetAllocationEntity allocation = service.allocateAsset(
                8L, 12L, selectedAllocationDate, " New laptop ", "infra.admin", "192.0.2.11");

        assertThat(asset.getStatus()).isEqualTo("ALLOCATED");
        assertThat(allocation.getEmployee()).isSameAs(employee);
        assertThat(allocation.getAllocatedDate()).isEqualTo(selectedAllocationDate);
        assertThat(allocation.getAllocationStatus()).isEqualTo("ASSIGNED");
        assertThat(allocation.getRemarks()).isEqualTo("New laptop");
    }

    @Test
    void requiresAUserSelectedNonFutureAllocationDate() {
        assertThatThrownBy(() -> service.allocateAsset(
                8L, 12L, null, null, "infra.admin", "192.0.2.11"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Asset, employee and allocation date are required.");

        assertThatThrownBy(() -> service.allocateAsset(
                8L, 12L, LocalDate.now().plusDays(1), null, "infra.admin", "192.0.2.11"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Allocation date cannot be in the future.");
    }

    @Test
    void returnsAssetWithoutWritingNullIntoRemarks() {
        AssetEntity asset = new AssetEntity();
        asset.setStatus("ALLOCATED");
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeCode("EMP-18");
        AssetAllocationEntity allocation = new AssetAllocationEntity();
        allocation.setId(18L);
        allocation.setAsset(asset);
        allocation.setEmployee(employee);
        allocation.setAllocationStatus("ASSIGNED");
        when(allocationRepository.findById(18L)).thenReturn(Optional.of(allocation));

        service.returnAsset(18L, " Working condition ", "infra.admin", "192.0.2.12");

        assertThat(asset.getStatus()).isEqualTo("AVAILABLE");
        assertThat(allocation.getAllocationStatus()).isEqualTo("RETURNED");
        assertThat(allocation.getRemarks()).isEqualTo("Return remarks: Working condition");
    }

    @Test
    void replacesAllocatedAssetAtomicallyAndKeepsAnAuditableHistory() {
        AssetEntity oldAsset = new AssetEntity();
        oldAsset.setId(8L);
        oldAsset.setAssetCode("OLD-008");
        oldAsset.setAssetName("Old Laptop");
        oldAsset.setOwnershipType(AssetOwnershipType.RENTED);
        oldAsset.setStatus("ALLOCATED");
        AssetEntity newAsset = new AssetEntity();
        newAsset.setId(9L);
        newAsset.setAssetCode("NEW-009");
        newAsset.setStatus("AVAILABLE");
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeCode("EMP-18");
        AssetAllocationEntity currentAllocation = new AssetAllocationEntity();
        currentAllocation.setId(18L);
        currentAllocation.setAsset(oldAsset);
        currentAllocation.setEmployee(employee);
        currentAllocation.setAllocationStatus("ASSIGNED");

        when(allocationRepository.findByIdForUpdate(18L)).thenReturn(Optional.of(currentAllocation));
        when(assetRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(oldAsset));
        when(assetRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(newAsset));
        when(allocationRepository.save(org.mockito.ArgumentMatchers.any(AssetAllocationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AssetAllocationEntity replacement = service.replaceAsset(
                18L, 9L, " Device repeatedly fails ", "infra.admin", "192.0.2.13");

        assertThat(currentAllocation.getAllocationStatus()).isEqualTo("REPLACED");
        assertThat(currentAllocation.getReturnedDate()).isNotNull();
        assertThat(currentAllocation.getRemarks()).contains("Device repeatedly fails");
        assertThat(oldAsset.getStatus()).isEqualTo("AVAILABLE");
        assertThat(newAsset.getStatus()).isEqualTo("ALLOCATED");
        assertThat(replacement.getAsset()).isSameAs(newAsset);
        assertThat(replacement.getEmployee()).isSameAs(employee);
        assertThat(replacement.getReplacesAllocation()).isSameAs(currentAllocation);
        assertThat(replacement.getAllocationStatus()).isEqualTo("ASSIGNED");

        ArgumentCaptor<AssetAuditLogEntity> auditCaptor = ArgumentCaptor.forClass(AssetAuditLogEntity.class);
        verify(auditLogRepository, times(2)).save(auditCaptor.capture());
        assertThat(auditCaptor.getAllValues())
                .extracting(AssetAuditLogEntity::getActionType)
                .containsExactly("REPLACED_OUT", "REPLACED_IN");
        assertThat(auditCaptor.getAllValues())
                .extracting(AssetAuditLogEntity::getDetails)
                .allSatisfy(details -> assertThat(details)
                        .contains("Old Laptop", "OLD-008", "RENTED", "Device repeatedly fails"));
    }

    @Test
    void rejectsReplacementReasonsThatExceedTheStoredLimit() {
        assertThatThrownBy(() -> service.replaceAsset(18L, 9L, "x".repeat(1001), "admin", "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Replacement reason must not exceed 1000 characters.");
    }

    @Test
    void validatesRentedAssetDatesAndAllowsAnOpenEndedRental() {
        AssetCategoryEntity category = new AssetCategoryEntity();
        category.setId(3L);
        category.setIsActive(true);
        AssetEntity asset = new AssetEntity();
        asset.setAssetCategory(category);
        asset.setAssetCode("RTR-1");
        asset.setAssetName("Rented Router");
        asset.setMakeModel("Cisco ISR 1100");
        asset.setOwnershipType(AssetOwnershipType.RENTED);
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.createAsset(asset, "admin", "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rent start date is required for a rented asset.");

        asset.setRentStartDate(LocalDate.of(2026, 10, 12));
        asset.setRentEndDate(LocalDate.of(2026, 10, 11));
        assertThatThrownBy(() -> service.createAsset(asset, "admin", "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rent end date cannot be before the rent start date.");

        asset.setRentEndDate(null);
        asset.setPurchaseDate(LocalDate.of(2026, 10, 1));
        asset.setWarrantyExpiryDate(LocalDate.of(2027, 10, 1));
        when(assetRepository.save(asset)).thenReturn(asset);
        AssetEntity savedAsset = service.createAsset(asset, "admin", "127.0.0.1");
        assertThat(savedAsset.getRentEndDate()).isNull();
        assertThat(savedAsset.getPurchaseDate()).isNull();
        assertThat(savedAsset.getWarrantyExpiryDate()).isNull();
    }
}
