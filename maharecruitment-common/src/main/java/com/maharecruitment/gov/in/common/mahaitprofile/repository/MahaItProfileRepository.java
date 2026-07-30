package com.maharecruitment.gov.in.common.mahaitprofile.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maharecruitment.gov.in.common.mahaitprofile.entity.MahaItProfile;

public interface MahaItProfileRepository extends JpaRepository<MahaItProfile, Long> {

    Optional<MahaItProfile> findFirstByActiveTrueOrderByUpdatedDateDesc();

    Optional<MahaItProfile> findFirstByOrderByUpdatedDateDesc();
}
