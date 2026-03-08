package com.maharecruitment.gov.in.master.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.maharecruitment.gov.in.master.entity.AgencyMaster;

public interface AgencyMasterRepository extends JpaRepository<AgencyMaster, Long> {

    @EntityGraph(attributePaths = "escalationMatrixEntries")
    @Query("""
            select a
            from AgencyMaster a
            where a.agencyId = :agencyId
            """)
    Optional<AgencyMaster> findDetailedByAgencyId(@Param("agencyId") Long agencyId);

    @Query("""
            select (count(a) > 0) from AgencyMaster a
            where lower(a.agencyName) = lower(:agencyName)
              and (:excludeId is null or a.agencyId <> :excludeId)
            """)
    boolean existsByAgencyNameExcludingId(@Param("agencyName") String agencyName, @Param("excludeId") Long excludeId);

    @Query("""
            select (count(a) > 0) from AgencyMaster a
            where lower(a.officialEmail) = lower(:officialEmail)
              and (:excludeId is null or a.agencyId <> :excludeId)
            """)
    boolean existsByOfficialEmailExcludingId(@Param("officialEmail") String officialEmail, @Param("excludeId") Long excludeId);

    @Query("""
            select (count(a) > 0) from AgencyMaster a
            where a.panNumber = :panNumber
              and (:excludeId is null or a.agencyId <> :excludeId)
            """)
    boolean existsByPanNumberExcludingId(@Param("panNumber") String panNumber, @Param("excludeId") Long excludeId);

    @Query("""
            select (count(a) > 0) from AgencyMaster a
            where a.gstNumber = :gstNumber
              and (:excludeId is null or a.agencyId <> :excludeId)
            """)
    boolean existsByGstNumberExcludingId(@Param("gstNumber") String gstNumber, @Param("excludeId") Long excludeId);
}
