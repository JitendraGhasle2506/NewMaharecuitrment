package com.maharecruitment.gov.in.asset.service.model;

public record AssetDashboardSummary(
        long totalAssets,
        long allocatedAssets,
        long damagedOrLostAssets,
        long availableAssets) {
}
