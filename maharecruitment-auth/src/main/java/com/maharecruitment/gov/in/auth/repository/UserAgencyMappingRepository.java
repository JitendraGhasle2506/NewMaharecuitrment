package com.maharecruitment.gov.in.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.auth.entity.UserAgencyMappingEntity;

@Repository
public interface UserAgencyMappingRepository extends JpaRepository<UserAgencyMappingEntity, Long> {

    List<UserAgencyMappingEntity> findByUser_IdOrderByUserAgencyMappingIdAsc(Long userId);

    Optional<UserAgencyMappingEntity> findByUser_IdAndAgencyId(Long userId, Long agencyId);

    Optional<UserAgencyMappingEntity> findTopByUser_IdAndActiveTrueOrderByPrimaryMappingDescUserAgencyMappingIdAsc(
            Long userId);
}
