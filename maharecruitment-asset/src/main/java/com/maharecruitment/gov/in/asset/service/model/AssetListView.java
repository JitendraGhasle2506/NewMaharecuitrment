package com.maharecruitment.gov.in.asset.service.model;

import java.time.LocalDate;

import com.maharecruitment.gov.in.asset.entity.AssetOwnershipType;

public record AssetListView(
        Long id,
        String assetCode,
        String assetName,
        String makeModel,
        String categoryName,
        AssetOwnershipType ownershipType,
        LocalDate rentStartDate,
        LocalDate rentEndDate,
        LocalDate purchaseDate,
        LocalDate warrantyExpiryDate,
        String status) {
}
