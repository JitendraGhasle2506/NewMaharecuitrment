package com.maharecruitment.gov.in.master.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.master.entity.WingMaster;

@Repository
public interface WingMasterRepository extends JpaRepository<WingMaster, Long> {

    boolean existsByWingNameIgnoreCase(String wingName);

    boolean existsByWingNameIgnoreCaseAndWingIdNot(String wingName, Long wingId);

    Optional<WingMaster> findByWingId(Long wingId);

    List<WingMaster> findAllByOrderByWingNameAsc();

    List<WingMaster> findByActiveFlagIgnoreCaseOrderByWingNameAsc(String activeFlag);

    Page<WingMaster> findByActiveFlagIgnoreCase(String activeFlag, Pageable pageable);

    Page<WingMaster> findByWingNameContainingIgnoreCase(String wingName, Pageable pageable);

    Page<WingMaster> findByActiveFlagIgnoreCaseAndWingNameContainingIgnoreCase(
            String activeFlag,
            String wingName,
            Pageable pageable);
}
