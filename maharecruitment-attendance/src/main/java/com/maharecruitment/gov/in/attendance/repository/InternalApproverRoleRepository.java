package com.maharecruitment.gov.in.attendance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.maharecruitment.gov.in.attendance.entity.InternalApproverRoleEntity;

public interface InternalApproverRoleRepository extends JpaRepository<InternalApproverRoleEntity, Long> {

    /** Names of the roles allowed to decide INTERNAL employees' requests, sorted. */
    @Query("""
            select r.name from Role r
            where r.id in (select a.roleId from InternalApproverRoleEntity a)
            order by r.name
            """)
    List<String> findApproverRoleNames();
}
