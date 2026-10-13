package com.maharecruitment.gov.in.asset.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.asset.entity.AssetAllocationEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface AssetAllocationRepository extends JpaRepository<AssetAllocationEntity, Long> {
    @EntityGraph(attributePaths = { "asset", "asset.assetCategory", "employee" })
    Page<AssetAllocationEntity> findByAllocationStatus(String allocationStatus, Pageable pageable);

    @Query(value = """
            select allocation
            from AssetAllocationEntity allocation
            join fetch allocation.asset asset
            join fetch asset.assetCategory category
            join fetch allocation.employee employee
            where allocation.allocationStatus = 'ASSIGNED'
              and (
                   lower(asset.assetCode) like concat('%', :searchTerm, '%')
                   or lower(coalesce(asset.makeModel, '')) like concat('%', :searchTerm, '%')
                   or lower(employee.fullName) like concat('%', :searchTerm, '%')
                   or lower(category.categoryName) like concat('%', :searchTerm, '%')
              )
            """,
            countQuery = """
                    select count(allocation)
                    from AssetAllocationEntity allocation
                    join allocation.asset asset
                    join asset.assetCategory category
                    join allocation.employee employee
                    where allocation.allocationStatus = 'ASSIGNED'
                      and (
                           lower(asset.assetCode) like concat('%', :searchTerm, '%')
                           or lower(coalesce(asset.makeModel, '')) like concat('%', :searchTerm, '%')
                           or lower(employee.fullName) like concat('%', :searchTerm, '%')
                           or lower(category.categoryName) like concat('%', :searchTerm, '%')
                      )
                    """)
    Page<AssetAllocationEntity> searchActiveAllocations(
            @Param("searchTerm") String searchTerm, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select allocation from AssetAllocationEntity allocation where allocation.id = :allocationId")
    Optional<AssetAllocationEntity> findByIdForUpdate(@Param("allocationId") Long allocationId);
}
