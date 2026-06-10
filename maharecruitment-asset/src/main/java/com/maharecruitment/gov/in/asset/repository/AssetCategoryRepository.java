package com.maharecruitment.gov.in.asset.repository;

import com.maharecruitment.gov.in.asset.entity.AssetCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetCategoryRepository extends JpaRepository<AssetCategoryEntity, Long> {
    List<AssetCategoryEntity> findByIsActiveTrue();
}
