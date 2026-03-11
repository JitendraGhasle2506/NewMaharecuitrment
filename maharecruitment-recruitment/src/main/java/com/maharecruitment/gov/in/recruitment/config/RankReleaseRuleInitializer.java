package com.maharecruitment.gov.in.recruitment.config;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.recruitment.entity.RankReleaseRuleEntity;
import com.maharecruitment.gov.in.recruitment.repository.RankReleaseRuleRepository;

@Component
@Order(60)
public class RankReleaseRuleInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RankReleaseRuleInitializer.class);

    private final RankReleaseRuleRepository rankReleaseRuleRepository;

    public RankReleaseRuleInitializer(RankReleaseRuleRepository rankReleaseRuleRepository) {
        this.rankReleaseRuleRepository = rankReleaseRuleRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedDefaultRuleIfMissing(1, 0);
        seedDefaultRuleIfMissing(2, 3);
        seedDefaultRuleIfMissing(3, 3);

        List<RankReleaseRuleEntity> rules = rankReleaseRuleRepository.findAllByOrderByRankNumberAsc();
        log.info("Rank release rules loaded. count={}", rules.size());
    }

    private void seedDefaultRuleIfMissing(int rankNumber, int releaseAfterDays) {
        if (rankReleaseRuleRepository.existsByRankNumber(rankNumber)) {
            return;
        }

        RankReleaseRuleEntity rule = new RankReleaseRuleEntity();
        rule.setRankNumber(rankNumber);
        rule.setReleaseAfterDays(releaseAfterDays);
        rule.setDelayFromPreviousRankDays(releaseAfterDays);
        rule.setEffectiveFrom(LocalDate.now());
        rule.setEffectiveTo(LocalDate.of(9999, 12, 31));
        rule.setIsActive(Boolean.TRUE);
        rankReleaseRuleRepository.save(rule);

        log.info(
                "Default rank release rule created. rankNumber={}, releaseAfterDays={}",
                rankNumber,
                releaseAfterDays);
    }
}
