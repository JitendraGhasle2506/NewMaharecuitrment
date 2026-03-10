package com.maharecruitment.gov.in.department.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.department.entity.DepartmentTaxRateMasterEntity;

@Repository
public interface DepartmentTaxRateMasterRepository extends JpaRepository<DepartmentTaxRateMasterEntity, Long> {

    Optional<DepartmentTaxRateMasterEntity> findFirstByTaxCodeIgnoreCaseAndEffectiveFromOrderByDepartmentTaxRateMasterIdAsc(
            String taxCode,
            LocalDate effectiveFrom);

    @Query(
            "select t "
                    + "from DepartmentTaxRateMasterEntity t "
                    + "where t.active = true "
                    + "and t.effectiveFrom <= :applicableDate "
                    + "and (t.effectiveTo is null or t.effectiveTo >= :applicableDate) "
                    + "order by t.taxCode asc")
    List<DepartmentTaxRateMasterEntity> findApplicableTaxRates(@Param("applicableDate") LocalDate applicableDate);
}
