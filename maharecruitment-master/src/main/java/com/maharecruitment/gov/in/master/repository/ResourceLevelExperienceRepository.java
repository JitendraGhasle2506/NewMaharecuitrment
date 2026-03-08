package com.maharecruitment.gov.in.master.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.master.entity.ResourceLevelExperience;

@Repository
public interface ResourceLevelExperienceRepository extends JpaRepository<ResourceLevelExperience, Long> {

    Page<ResourceLevelExperience> findByActiveFlagIgnoreCase(String activeFlag, Pageable pageable);

    Optional<ResourceLevelExperience> findByLevelIdAndActiveFlagIgnoreCase(Long levelId, String activeFlag);

    Optional<ResourceLevelExperience> findByLevelCodeIgnoreCaseAndActiveFlagIgnoreCase(String levelCode,
            String activeFlag);

    Optional<ResourceLevelExperience> findByLevelCodeIgnoreCase(String levelCode);

    boolean existsByLevelCodeIgnoreCaseAndActiveFlagIgnoreCase(String levelCode, String activeFlag);

    boolean existsByLevelCodeIgnoreCase(String levelCode);

    List<ResourceLevelExperience> findByLevelIdInAndActiveFlagIgnoreCase(Collection<Long> levelIds, String activeFlag);

    @Query("""
            SELECT COUNT(r) > 0
            FROM ResourceLevelExperience r
            WHERE LOWER(r.levelCode) = LOWER(:levelCode)
              AND (:excludeId IS NULL OR r.levelId <> :excludeId)
            """)
    boolean existsByLevelCodeExcludingId(@Param("levelCode") String levelCode, @Param("excludeId") Long excludeId);
}
