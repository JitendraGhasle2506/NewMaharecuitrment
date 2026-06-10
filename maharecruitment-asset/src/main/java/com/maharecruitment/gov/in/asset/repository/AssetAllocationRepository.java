package com.maharecruitment.gov.in.asset.repository;

import com.maharecruitment.gov.in.asset.entity.AssetAllocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetAllocationRepository extends JpaRepository<AssetAllocationEntity, Long> {
    List<AssetAllocationEntity> findByEmployeeEmployeeId(Long employeeId);
    List<AssetAllocationEntity> findByEmployeeEmployeeIdAndAllocationStatus(Long employeeId, String allocationStatus);
}
