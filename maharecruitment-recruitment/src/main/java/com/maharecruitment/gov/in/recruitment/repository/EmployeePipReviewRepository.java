package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeePipReview;

public interface EmployeePipReviewRepository extends JpaRepository<EmployeePipReview, Long> {
    List<EmployeePipReview> findByPlanIdOrderByReviewDateDescIdDesc(Long planId);
}
