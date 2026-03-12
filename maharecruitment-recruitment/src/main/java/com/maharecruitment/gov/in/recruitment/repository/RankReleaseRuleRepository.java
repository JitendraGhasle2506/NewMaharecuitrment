package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.RankReleaseRuleEntity;

@Repository
public interface RankReleaseRuleRepository extends JpaRepository<RankReleaseRuleEntity, Long> {

    Optional<RankReleaseRuleEntity> findByRankNumber(Integer rankNumber);

    boolean existsByRankNumber(Integer rankNumber);

    List<RankReleaseRuleEntity> findAllByOrderByRankNumberAsc();
}
