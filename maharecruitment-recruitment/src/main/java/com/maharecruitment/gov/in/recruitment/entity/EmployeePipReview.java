package com.maharecruitment.gov.in.recruitment.entity;

import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Append-only reviews retain each manager's dated feedback rather than overwriting earlier feedback. */
@Entity
@Table(name = "employee_pip_review")
@Getter
@Setter
public class EmployeePipReview extends RecruitmentAuditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "plan_id", nullable = false)
    private Long planId;
    @Column(name = "reviewer_user_id", nullable = false)
    private Long reviewerUserId;
    @Column(name = "reviewer_name", nullable = false, length = 255)
    private String reviewerName;
    @Column(name = "review_date", nullable = false)
    private LocalDate reviewDate;
    @Column(nullable = false)
    private int rating;
    @Column(nullable = false, length = 2000)
    private String feedback;
}
