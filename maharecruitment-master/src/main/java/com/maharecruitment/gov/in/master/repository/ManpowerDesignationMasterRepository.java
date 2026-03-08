package com.maharecruitment.gov.in.master.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;

@Repository
public interface ManpowerDesignationMasterRepository extends JpaRepository<ManpowerDesignationMaster, Long> {

    Page<ManpowerDesignationMaster> findByActiveFlagIgnoreCase(String activeFlag, Pageable pageable);

    Optional<ManpowerDesignationMaster> findByDesignationIdAndActiveFlagIgnoreCase(Long designationId, String activeFlag);

    @Query("""
            SELECT COUNT(d) > 0
            FROM ManpowerDesignationMaster d
            WHERE LOWER(d.category) = LOWER(:category)
              AND LOWER(d.designationName) = LOWER(:designationName)
              AND (:excludeId IS NULL OR d.designationId <> :excludeId)
            """)
    boolean existsByCategoryAndDesignationNameExcludingId(
            @Param("category") String category,
            @Param("designationName") String designationName,
            @Param("excludeId") Long excludeId);
}
