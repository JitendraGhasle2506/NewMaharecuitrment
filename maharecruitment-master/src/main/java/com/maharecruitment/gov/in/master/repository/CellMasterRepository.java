package com.maharecruitment.gov.in.master.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.master.entity.CellMaster;

@Repository
public interface CellMasterRepository extends JpaRepository<CellMaster, Long> {

    boolean existsByCellNameIgnoreCase(String cellName);

    boolean existsByCellNameIgnoreCaseAndCellIdNot(String cellName, Long cellId);

    Optional<CellMaster> findByCellId(Long cellId);

    List<CellMaster> findAllByOrderByCellNameAsc();

    List<CellMaster> findByActiveFlagIgnoreCaseOrderByCellNameAsc(String activeFlag);

    Page<CellMaster> findByActiveFlagIgnoreCase(String activeFlag, Pageable pageable);

    Page<CellMaster> findByCellNameContainingIgnoreCase(String cellName, Pageable pageable);

    Page<CellMaster> findByActiveFlagIgnoreCaseAndCellNameContainingIgnoreCase(
            String activeFlag,
            String cellName,
            Pageable pageable);
}
