package com.maharecruitment.gov.in.web.service.pip.impl;

import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeePipPlan.Status;
import com.maharecruitment.gov.in.recruitment.repository.EmployeePipPlanRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeePipReportRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeePipReportRepository.Row;
import com.maharecruitment.gov.in.recruitment.repository.EmployeePipReviewRepository;
import com.maharecruitment.gov.in.web.service.pip.EmployeePipService.Detail;
import com.maharecruitment.gov.in.web.service.pip.EmployeePipService.PlanView;
import com.maharecruitment.gov.in.web.service.pip.EmployeePipService.ReviewView;
import com.maharecruitment.gov.in.web.service.pip.HrPipReportService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("hasAuthority('ROLE_HR')")
public class HrPipReportServiceImpl implements HrPipReportService {
    private final UserRepository users;
    private final EmployeePipReportRepository reports;
    private final EmployeePipPlanRepository plans;
    private final EmployeePipReviewRepository reviews;

    @Override public Overview overview(String login, Status status, String search, int page) {
        requireActiveAccount(login);
        if (page < 0 || (search != null && search.length() > 100)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a valid page and a search up to 100 characters.");
        }
        String term = search == null ? "" : search.strip().toLowerCase(Locale.ROOT);
        String pattern = "%" + term.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        var result = reports.findOverview(status, pattern, PageRequest.of(page, 12)).map(this::view);
        long underReview = 0, completed = 0, exited = 0;
        for (var count : reports.countPlansByStatus()) {
            switch (count.getStatus()) {
                case UNDER_REVIEW -> underReview = count.getTotal();
                case COMPLETED -> completed = count.getTotal();
                case EXIT_RECORDED -> exited = count.getTotal();
            }
        }
        return new Overview(result, underReview, completed, exited);
    }

    @Override public Detail detail(String login, Long id) {
        requireActiveAccount(login);
        var row = reports.findSummaryById(id).orElseThrow(this::notFound);
        var plan = plans.findById(id).orElseThrow(this::notFound);
        return new Detail(view(row), plan.getObjectives(), reviews.findByPlanIdOrderByReviewDateDescIdDesc(id).stream()
                .map(r -> new ReviewView(r.getReviewerName(), r.getReviewDate(), r.getRating(), r.getFeedback())).toList(),
                plan.getDecisionFeedback(), plan.getExitDate(), "ACTIVE".equalsIgnoreCase(row.getEmployeeStatus()), plan.getReason());
    }

    private PlanView view(Row row) {
        return new PlanView(row.getId(), row.getEmployeeId(), row.getName(), row.getCode(), row.getStartDate(),
                row.getEndDate(), row.getDurationMonths(), row.getStatus());
    }
    private void requireActiveAccount(String login) {
        if (!StringUtils.hasText(login) || !users.existsByEmailIgnoreCaseAndActiveTrue(login)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "An active HR account is required.");
        }
    }
    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "PIP plan not found.");
    }
}
