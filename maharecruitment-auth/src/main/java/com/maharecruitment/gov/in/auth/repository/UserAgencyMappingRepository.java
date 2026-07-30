package com.maharecruitment.gov.in.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.auth.entity.UserAgencyMappingEntity;

@Repository
public interface UserAgencyMappingRepository extends JpaRepository<UserAgencyMappingEntity, Long> {

    List<UserAgencyMappingEntity> findByUser_IdOrderByUserAgencyMappingIdAsc(Long userId);

    List<UserAgencyMappingEntity> findByAgencyId(Long agencyId);

    Optional<UserAgencyMappingEntity> findByUser_IdAndAgencyId(Long userId, Long agencyId);

    Optional<UserAgencyMappingEntity> findTopByUser_IdAndActiveTrueOrderByPrimaryMappingDescUserAgencyMappingIdAsc(
            Long userId);

    @Query(value = """
            select count(*) > 0
            from user_agency_mapping mapping
            join agency_master agency on agency.agency_id = mapping.agency_id
            where mapping.user_id = :userId
              and mapping.active = true
              and upper(trim(coalesce(agency.status, ''))) = 'ACTIVE'
            """, nativeQuery = true)
    boolean existsActiveAgencyForUserId(@Param("userId") Long userId);

    @Query(value = """
            select count(*) > 0
            from user_agency_mapping mapping
            where mapping.user_id = :userId
              and mapping.active = true
            """, nativeQuery = true)
    boolean existsActiveMappingForUserId(@Param("userId") Long userId);
}
