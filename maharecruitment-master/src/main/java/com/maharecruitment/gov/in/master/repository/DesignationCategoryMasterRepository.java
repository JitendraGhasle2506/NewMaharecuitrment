package com.maharecruitment.gov.in.master.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.master.entity.DesignationCategoryMaster;

@Repository
public interface DesignationCategoryMasterRepository extends JpaRepository<DesignationCategoryMaster, Long> {

    List<DesignationCategoryMaster> findByActiveFlag(String activeFlag);

    Optional<DesignationCategoryMaster> findByCategoryNameIgnoreCase(String categoryName);

    boolean existsByCategoryNameIgnoreCase(String categoryName);
}
