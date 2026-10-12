package com.maharecruitment.gov.in.asset.service.model;

import java.time.LocalDate;

import com.maharecruitment.gov.in.asset.entity.AssetOwnershipType;

public record AssetAllocationListView(
        Long allocationId,
        String assetName,
        String assetCode,
        String categoryName,
        AssetOwnershipType ownershipType,
        String employeeName,
        String employeeCode,
        LocalDate allocatedDate,
        String allocationStatus) {
}
