package com.maharecruitment.gov.in.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.auth.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("""
            select distinct u.id
            from User u
            join u.roles r
            where upper(trim(r.name)) = upper(trim(:roleName))
            """)
    List<Long> findDistinctUserIdsByRoleName(@Param("roleName") String roleName);

    @Query("""
            select distinct u.id
            from User u
            join u.roles r
            where u.departmentRegistrationId.departmentRegistrationId = :departmentRegistrationId
              and upper(trim(r.name)) = upper(trim(:roleName))
            """)
    List<Long> findDistinctUserIdsByDepartmentRegistrationIdAndRoleName(
            @Param("departmentRegistrationId") Long departmentRegistrationId,
            @Param("roleName") String roleName);

    @Query("""
            select distinct u.id
            from User u
            where u.departmentRegistrationId.departmentRegistrationId = :departmentRegistrationId
            """)
    List<Long> findDistinctUserIdsByDepartmentRegistrationId(
            @Param("departmentRegistrationId") Long departmentRegistrationId);
}
