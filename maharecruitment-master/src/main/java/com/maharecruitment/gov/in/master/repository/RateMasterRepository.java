package com.maharecruitment.gov.in.master.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.master.entity.RateMaster;

@Repository
public interface RateMasterRepository extends JpaRepository<RateMaster, Long> {

    Optional<RateMaster> findByTypeIgnoreCase(String type);

    boolean existsByTypeIgnoreCase(String type);
}
