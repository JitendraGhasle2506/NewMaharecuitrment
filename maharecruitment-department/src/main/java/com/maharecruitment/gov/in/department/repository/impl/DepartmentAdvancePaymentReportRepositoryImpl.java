package com.maharecruitment.gov.in.department.repository.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.repository.DepartmentAdvancePaymentReportRepository;
import com.maharecruitment.gov.in.department.service.model.ApprovedPaymentReportRowView;
import com.maharecruitment.gov.in.department.service.model.ApprovedPaymentReportSummaryView;
import com.maharecruitment.gov.in.department.service.model.DepartmentPaymentReportDepartmentOptionView;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Repository
public class DepartmentAdvancePaymentReportRepositoryImpl implements DepartmentAdvancePaymentReportRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<ApprovedPaymentReportRowView> findApprovedPaymentReportRows(
            DepartmentApplicationStatus approvedStatus,
            Long departmentRegistrationId,
            LocalDateTime approvedFrom,
            LocalDateTime approvedToExclusive,
            String searchPattern,
            Pageable pageable) {
        QueryParts queryParts = buildRowQuery(
                approvedStatus,
                departmentRegistrationId,
                approvedFrom,
                approvedToExclusive,
                searchPattern);

        TypedQuery<ApprovedPaymentReportRowView> query = entityManager.createQuery(
                queryParts.selectClause() + queryParts.fromWhereClause() + " order by p.updatedDate desc, p.id desc",
                ApprovedPaymentReportRowView.class);
        applyParameters(query, queryParts.parameters());
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        TypedQuery<Long> countQuery = entityManager.createQuery(
                "select count(p) " + queryParts.fromWhereClause(),
                Long.class);
        applyParameters(countQuery, queryParts.parameters());

        List<ApprovedPaymentReportRowView> content = query.getResultList();
        long total = countQuery.getSingleResult();
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public List<ApprovedPaymentReportRowView> findApprovedPaymentReportRows(
            DepartmentApplicationStatus approvedStatus,
            Long departmentRegistrationId,
            LocalDateTime approvedFrom,
            LocalDateTime approvedToExclusive,
            String searchPattern) {
        QueryParts queryParts = buildRowQuery(
                approvedStatus,
                departmentRegistrationId,
                approvedFrom,
                approvedToExclusive,
                searchPattern);

        TypedQuery<ApprovedPaymentReportRowView> query = entityManager.createQuery(
                queryParts.selectClause() + queryParts.fromWhereClause() + " order by p.updatedDate desc, p.id desc",
                ApprovedPaymentReportRowView.class);
        applyParameters(query, queryParts.parameters());
        return query.getResultList();
    }

    @Override
    public ApprovedPaymentReportSummaryView summarizeApprovedPaymentReport(
            DepartmentApplicationStatus approvedStatus,
            Long departmentRegistrationId,
            LocalDateTime approvedFrom,
            LocalDateTime approvedToExclusive,
            String searchPattern) {
        QueryParts queryParts = buildSummaryQuery(
                approvedStatus,
                departmentRegistrationId,
                approvedFrom,
                approvedToExclusive,
                searchPattern);

        TypedQuery<ApprovedPaymentReportSummaryView> query = entityManager.createQuery(
                queryParts.selectClause() + queryParts.fromWhereClause(),
                ApprovedPaymentReportSummaryView.class);
        applyParameters(query, queryParts.parameters());
        return query.getSingleResult();
    }

    @Override
    public List<DepartmentPaymentReportDepartmentOptionView> findApprovedPaymentDepartments(
            DepartmentApplicationStatus approvedStatus) {
        TypedQuery<DepartmentPaymentReportDepartmentOptionView> query = entityManager.createQuery(
                "select distinct new com.maharecruitment.gov.in.department.service.model.DepartmentPaymentReportDepartmentOptionView("
                        + "p.departmentRegistrationId, dept.departmentName) "
                        + "from DepartmentAdvancePaymentEntity p "
                        + "left join DepartmentRegistrationEntity dept "
                        + "on dept.departmentRegistrationId = p.departmentRegistrationId "
                        + "where p.applicationStatus = :approvedStatus "
                        + "order by dept.departmentName asc, p.departmentRegistrationId asc",
                DepartmentPaymentReportDepartmentOptionView.class);
        query.setParameter("approvedStatus", approvedStatus);
        return query.getResultList();
    }

    private QueryParts buildRowQuery(
            DepartmentApplicationStatus approvedStatus,
            Long departmentRegistrationId,
            LocalDateTime approvedFrom,
            LocalDateTime approvedToExclusive,
            String searchPattern) {
        String selectClause = "select new com.maharecruitment.gov.in.department.service.model.ApprovedPaymentReportRowView("
                + "p.id, "
                + "app.departmentProjectApplicationId, "
                + "app.requestId, "
                + "app.projectName, "
                + "app.projectCode, "
                + "p.departmentRegistrationId, "
                + "dept.departmentName, "
                + "app.workOrderNumber, "
                + "p.proformaInvoiceId, "
                + "p.receiptNumber, "
                + "p.utrNumber, "
                + "p.paymentMode, "
                + "p.totalAmount, "
                + "p.applicationStatus, "
                + "p.createdDate, "
                + "p.updatedDate, "
                + "p.updatedBy, "
                + "p.remarks) ";

        return new QueryParts(
                selectClause,
                buildFromWhereClause(
                        departmentRegistrationId,
                        approvedFrom,
                        approvedToExclusive,
                        searchPattern),
                buildParameters(
                        approvedStatus,
                        departmentRegistrationId,
                        approvedFrom,
                        approvedToExclusive,
                        searchPattern));
    }

    private QueryParts buildSummaryQuery(
            DepartmentApplicationStatus approvedStatus,
            Long departmentRegistrationId,
            LocalDateTime approvedFrom,
            LocalDateTime approvedToExclusive,
            String searchPattern) {
        String selectClause = "select new com.maharecruitment.gov.in.department.service.model.ApprovedPaymentReportSummaryView("
                + "count(p), "
                + "coalesce(sum(p.totalAmount), 0), "
                + "count(distinct p.departmentRegistrationId)) ";

        return new QueryParts(
                selectClause,
                buildFromWhereClause(
                        departmentRegistrationId,
                        approvedFrom,
                        approvedToExclusive,
                        searchPattern),
                buildParameters(
                        approvedStatus,
                        departmentRegistrationId,
                        approvedFrom,
                        approvedToExclusive,
                        searchPattern));
    }

    private String buildFromWhereClause(
            Long departmentRegistrationId,
            LocalDateTime approvedFrom,
            LocalDateTime approvedToExclusive,
            String searchPattern) {
        StringBuilder clause = new StringBuilder()
                .append("from DepartmentAdvancePaymentEntity p ")
                .append("join p.application app ")
                .append("left join DepartmentRegistrationEntity dept ")
                .append("on dept.departmentRegistrationId = p.departmentRegistrationId ")
                .append("where p.applicationStatus = :approvedStatus ");

        if (departmentRegistrationId != null) {
            clause.append("and p.departmentRegistrationId = :departmentRegistrationId ");
        }
        if (approvedFrom != null) {
            clause.append("and p.updatedDate >= :approvedFrom ");
        }
        if (approvedToExclusive != null) {
            clause.append("and p.updatedDate < :approvedToExclusive ");
        }
        if (hasText(searchPattern)) {
            clause.append("and (")
                    .append("lower(app.requestId) like :searchPattern ")
                    .append("or lower(app.projectName) like :searchPattern ")
                    .append("or lower(coalesce(app.projectCode, '')) like :searchPattern ")
                    .append("or lower(coalesce(dept.departmentName, '')) like :searchPattern ")
                    .append("or lower(cast(coalesce(p.receiptNumber, '') as string)) like :searchPattern ")
                    .append("or lower(cast(coalesce(p.utrNumber, '') as string)) like :searchPattern ")
                    .append("or lower(cast(coalesce(p.proformaInvoiceId, '') as string)) like :searchPattern")
                    .append(") ");
        }
        return clause.toString();
    }

    private Map<String, Object> buildParameters(
            DepartmentApplicationStatus approvedStatus,
            Long departmentRegistrationId,
            LocalDateTime approvedFrom,
            LocalDateTime approvedToExclusive,
            String searchPattern) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("approvedStatus", approvedStatus);
        if (departmentRegistrationId != null) {
            parameters.put("departmentRegistrationId", departmentRegistrationId);
        }
        if (approvedFrom != null) {
            parameters.put("approvedFrom", approvedFrom);
        }
        if (approvedToExclusive != null) {
            parameters.put("approvedToExclusive", approvedToExclusive);
        }
        if (hasText(searchPattern)) {
            parameters.put("searchPattern", searchPattern);
        }
        return parameters;
    }

    private void applyParameters(jakarta.persistence.Query query, Map<String, Object> parameters) {
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record QueryParts(
            String selectClause,
            String fromWhereClause,
            Map<String, Object> parameters) {
    }
}
