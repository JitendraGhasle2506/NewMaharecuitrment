package com.maharecruitment.gov.in.master.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.master.entity.CellMaster;

@Repository
public interface CellMasterRepository extends JpaRepository<CellMaster, Long> {

    boolean existsByCellNameIgnoreCase(String cellName);

    boolean existsByCellNameIgnoreCaseAndCellIdNot(String cellName, Long cellId);

    @EntityGraph(attributePaths = "wing")
    Optional<CellMaster> findByCellId(Long cellId);

    @EntityGraph(attributePaths = "wing")
    Optional<CellMaster> findFirstByCellNameIgnoreCaseAndActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCase(
            String cellName,
            String activeFlag,
            String wingActiveFlag);

    @EntityGraph(attributePaths = "wing")
    List<CellMaster> findAllByOrderByCellNameAsc();

    @EntityGraph(attributePaths = "wing")
    List<CellMaster> findByActiveFlagIgnoreCaseOrderByCellNameAsc(String activeFlag);

    @EntityGraph(attributePaths = "wing")
    List<CellMaster> findByActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCaseOrderByCellNameAsc(
            String activeFlag,
            String wingActiveFlag);

    @EntityGraph(attributePaths = "wing")
    List<CellMaster> findByWing_WingIdAndActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCaseOrderByCellNameAsc(
            Long wingId,
            String activeFlag,
            String wingActiveFlag);

    @EntityGraph(attributePaths = "wing")
    Page<CellMaster> findByActiveFlagIgnoreCase(String activeFlag, Pageable pageable);

    @EntityGraph(attributePaths = "wing")
    Page<CellMaster> findByActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCase(
            String activeFlag,
            String wingActiveFlag,
            Pageable pageable);

    @EntityGraph(attributePaths = "wing")
    Page<CellMaster> findByCellNameContainingIgnoreCase(String cellName, Pageable pageable);

    @EntityGraph(attributePaths = "wing")
    Page<CellMaster> findByActiveFlagIgnoreCaseAndCellNameContainingIgnoreCase(
            String activeFlag,
            String cellName,
            Pageable pageable);

    @EntityGraph(attributePaths = "wing")
    Page<CellMaster> findByActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCaseAndCellNameContainingIgnoreCase(
            String activeFlag,
            String wingActiveFlag,
            String cellName,
            Pageable pageable);

    @EntityGraph(attributePaths = "wing")
    Page<CellMaster> findByWing_WingId(Long wingId, Pageable pageable);

    @EntityGraph(attributePaths = "wing")
    Page<CellMaster> findByWing_WingIdAndActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCase(
            Long wingId,
            String activeFlag,
            String wingActiveFlag,
            Pageable pageable);

    @EntityGraph(attributePaths = "wing")
    Page<CellMaster> findByWing_WingIdAndCellNameContainingIgnoreCase(
            Long wingId,
            String cellName,
            Pageable pageable);

    @EntityGraph(attributePaths = "wing")
    Page<CellMaster> findByWing_WingIdAndActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCaseAndCellNameContainingIgnoreCase(
            Long wingId,
            String activeFlag,
            String wingActiveFlag,
            String cellName,
            Pageable pageable);
}
