package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.AgencyGlobalRankEntity;

@Repository
public interface AgencyGlobalRankRepository extends JpaRepository<AgencyGlobalRankEntity, Long> {

    @Query("select rank "
            + "from AgencyGlobalRankEntity rank "
            + "join fetch rank.agency agency "
            + "order by rank.rankNumber asc, agency.agencyId asc")
    List<AgencyGlobalRankEntity> findAllWithAgencyOrderByRankNumberAscAgencyAgencyIdAsc();

    Optional<AgencyGlobalRankEntity> findByAgencyAgencyId(Long agencyId);

    @Query("select rank "
            + "from AgencyGlobalRankEntity rank "
            + "join fetch rank.agency agency "
            + "where rank.rankNumber = :rankNumber "
            + "order by agency.agencyId asc")
    List<AgencyGlobalRankEntity> findByRankNumberWithAgencyOrderByAgencyAgencyIdAsc(
            @Param("rankNumber") Integer rankNumber);

    @Query("select min(rank.rankNumber) from AgencyGlobalRankEntity rank")
    Integer findMinimumRank();

    @Query("select min(rank.rankNumber) "
            + "from AgencyGlobalRankEntity rank "
            + "where rank.rankNumber > :currentRank")
    Integer findNextHigherRank(@Param("currentRank") Integer currentRank);
}
