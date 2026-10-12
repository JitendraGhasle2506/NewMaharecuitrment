package com.maharecruitment.gov.in.asset.repository;

import com.maharecruitment.gov.in.asset.entity.AssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<AssetEntity, Long> {
    List<AssetEntity> findByStatus(String status);
    List<AssetEntity> findByAssetCategoryId(Long categoryId);

    boolean existsByAssetCodeIgnoreCase(String assetCode);

    @EntityGraph(attributePaths = "assetCategory")
    List<AssetEntity> findAllByOrderByAssetCodeAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select asset from AssetEntity asset where asset.id = :assetId")
    Optional<AssetEntity> findByIdForUpdate(@Param("assetId") Long assetId);
}
