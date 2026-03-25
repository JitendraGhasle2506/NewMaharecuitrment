package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningEntity;
import com.maharecruitment.gov.in.recruitment.repository.projection.InternalVacancyOpeningStatusCountProjection;

@Repository
public interface InternalVacancyOpeningRepository extends JpaRepository<InternalVacancyOpeningEntity, Long> {

    @EntityGraph(attributePaths = { "projectMst", "requirements", "requirements.designationMst" })
    List<InternalVacancyOpeningEntity> findAllByOrderByInternalVacancyOpeningIdDesc();

    @EntityGraph(attributePaths = { "projectMst", "requirements", "requirements.designationMst" })
    Optional<InternalVacancyOpeningEntity> findDetailedByInternalVacancyOpeningId(Long internalVacancyOpeningId);

    @EntityGraph(attributePaths = { "projectMst" })
    @Query(value = "select opening "
            + "from InternalVacancyOpeningEntity opening "
            + "join opening.projectMst project "
            + "where (:searchPattern is null "
            + "or upper(opening.requestId) like :searchPattern "
            + "or upper(project.projectName) like :searchPattern)",
            countQuery = "select count(opening) "
                    + "from InternalVacancyOpeningEntity opening "
                    + "join opening.projectMst project "
                    + "where (:searchPattern is null "
                    + "or upper(opening.requestId) like :searchPattern "
                    + "or upper(project.projectName) like :searchPattern)")
    Page<InternalVacancyOpeningEntity> findPageBySearch(
            @Param("searchPattern") String searchPattern,
            Pageable pageable);

    @Query("select opening.status as status, count(opening) as totalCount "
            + "from InternalVacancyOpeningEntity opening "
            + "join opening.projectMst project "
            + "where (:searchPattern is null "
            + "or upper(opening.requestId) like :searchPattern "
            + "or upper(project.projectName) like :searchPattern) "
            + "group by opening.status")
    List<InternalVacancyOpeningStatusCountProjection> summarizeStatusCounts(
            @Param("searchPattern") String searchPattern);
}
