package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningEntity;

@Repository
public interface InternalVacancyOpeningRepository extends JpaRepository<InternalVacancyOpeningEntity, Long> {

    @EntityGraph(attributePaths = { "projectMst", "requirements", "requirements.designationMst" })
    List<InternalVacancyOpeningEntity> findAllByOrderByInternalVacancyOpeningIdDesc();

    @EntityGraph(attributePaths = { "projectMst", "requirements", "requirements.designationMst" })
    Optional<InternalVacancyOpeningEntity> findDetailedByInternalVacancyOpeningId(Long internalVacancyOpeningId);
}
