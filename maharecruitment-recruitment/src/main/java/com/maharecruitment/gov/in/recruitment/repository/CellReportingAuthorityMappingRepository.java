package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.CellReportingAuthorityMappingEntity;

@Repository
public interface CellReportingAuthorityMappingRepository
        extends JpaRepository<CellReportingAuthorityMappingEntity, Long> {

    @EntityGraph(attributePaths = { "cell", "cell.wing" })
    Optional<CellReportingAuthorityMappingEntity> findByCellCellId(Long cellId);

    @EntityGraph(attributePaths = { "cell", "cell.wing" })
    List<CellReportingAuthorityMappingEntity> findAllByOrderByCellCellNameAsc();

    @Query("""
            select mapping.cell.cellId
            from CellReportingAuthorityMappingEntity mapping
            where mapping.authorityUserId = :authorityUserId
            """)
    List<Long> findCellIdsByAuthorityUserId(@Param("authorityUserId") Long authorityUserId);
}
