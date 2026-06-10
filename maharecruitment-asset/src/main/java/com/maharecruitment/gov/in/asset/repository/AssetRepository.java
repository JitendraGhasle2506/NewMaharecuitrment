package com.maharecruitment.gov.in.asset.repository;

import com.maharecruitment.gov.in.asset.entity.AssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetRepository extends JpaRepository<AssetEntity, Long> {
    List<AssetEntity> findByStatus(String status);
    List<AssetEntity> findByAssetCategoryId(Long categoryId);
}
