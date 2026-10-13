package com.maharecruitment.gov.in.asset.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.asset.entity.AssetEntity;
import com.maharecruitment.gov.in.asset.service.model.AssetDashboardSummary;
import com.maharecruitment.gov.in.asset.service.model.AssetOptionView;

import jakarta.persistence.LockModeType;

@Repository
public interface AssetRepository extends JpaRepository<AssetEntity, Long> {
    boolean existsByAssetCodeIgnoreCase(String assetCode);

    @EntityGraph(attributePaths = "assetCategory")
    Page<AssetEntity> findAll(Pageable pageable);

    @Query("""
            select new com.maharecruitment.gov.in.asset.service.model.AssetDashboardSummary(
                count(asset),
                coalesce(sum(case when asset.status = 'ALLOCATED' then 1 else 0 end), 0),
                coalesce(sum(case when asset.status in ('DAMAGED', 'LOST') then 1 else 0 end), 0),
                coalesce(sum(case when asset.status = 'AVAILABLE' then 1 else 0 end), 0))
            from AssetEntity asset
            """)
    AssetDashboardSummary getDashboardSummary();

    @Query("""
            select new com.maharecruitment.gov.in.asset.service.model.AssetOptionView(
                asset.id, asset.assetCode, asset.assetName, asset.makeModel)
            from AssetEntity asset
            where asset.status = 'AVAILABLE'
            order by asset.assetCode
            """)
    List<AssetOptionView> findAvailableOptions();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select asset from AssetEntity asset where asset.id = :assetId")
    Optional<AssetEntity> findByIdForUpdate(@Param("assetId") Long assetId);
}
