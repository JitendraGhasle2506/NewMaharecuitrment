package com.maharecruitment.gov.in.recruitment.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import com.maharecruitment.gov.in.recruitment.entity.EmployeePipPlan;
import com.maharecruitment.gov.in.recruitment.entity.EmployeePipPlan.Status;

/** Organisation-wide, read-only projections. Closed plans and exited employees remain reportable. */
public interface EmployeePipReportRepository extends Repository<EmployeePipPlan, Long> {
    String SUMMARY = """
            select p.id as id, p.employeeId as employeeId, e.fullName as name, e.employeeCode as code,
                   e.status as employeeStatus, p.startDate as startDate, p.endDate as endDate,
                   p.durationMonths as durationMonths, p.status as status
            from EmployeePipPlan p, EmployeeEntity e
            where e.employeeId = p.employeeId
            """;

    @Query(value = SUMMARY + """
            and (:status is null or p.status = :status)
            and lower(concat(coalesce(e.fullName, ''), ' ', coalesce(e.employeeCode, ''))) like :search escape '!'
            order by p.createdDateTime desc, p.id desc
            """, countQuery = """
            select count(p) from EmployeePipPlan p, EmployeeEntity e
            where e.employeeId = p.employeeId and (:status is null or p.status = :status)
            and lower(concat(coalesce(e.fullName, ''), ' ', coalesce(e.employeeCode, ''))) like :search escape '!'
            """)
    Page<Row> findOverview(@Param("status") Status status, @Param("search") String search, Pageable pageable);

    @Query(SUMMARY + " and p.id = :id")
    Optional<Row> findSummaryById(@Param("id") Long id);

    @Query("select p.status as status, count(p) as total from EmployeePipPlan p group by p.status")
    List<StatusCount> countPlansByStatus();

    interface Row {
        Long getId();
        Long getEmployeeId();
        String getName();
        String getCode();
        String getEmployeeStatus();
        LocalDate getStartDate();
        LocalDate getEndDate();
        int getDurationMonths();
        Status getStatus();
    }
    interface StatusCount {
        Status getStatus();
        long getTotal();
    }
}
