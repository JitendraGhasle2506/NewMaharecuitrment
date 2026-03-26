package com.maharecruitment.gov.in.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

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

    @Query(
            value = """
                    select distinct u.id
                    from users u
                    join users_roles ur on ur.user_id = u.id
                    join roles r on r.id = ur.role_id
                    left join user_department_mapping udm
                        on udm.user_id = u.id
                       and udm.active = true
                    where (
                            u.department_registration_id = :departmentRegistrationId
                            or udm.department_registration_id = :departmentRegistrationId
                    )
                      and upper(trim(r.name)) = upper(trim(:roleName))
                    """,
            nativeQuery = true)
    List<Long> findDistinctUserIdsByDepartmentRegistrationIdAndRoleName(
            @Param("departmentRegistrationId") Long departmentRegistrationId,
            @Param("roleName") String roleName);

    @Query(
            value = """
                    select distinct u.id
                    from users u
                    left join user_department_mapping udm
                        on udm.user_id = u.id
                       and udm.active = true
                    where u.department_registration_id = :departmentRegistrationId
                       or udm.department_registration_id = :departmentRegistrationId
                    """,
            nativeQuery = true)
    List<Long> findDistinctUserIdsByDepartmentRegistrationId(
            @Param("departmentRegistrationId") Long departmentRegistrationId);

    @Query("""
            select distinct u
            from User u
            join fetch u.roles r
            where r.id in :roleIds
            """)
    List<User> findDistinctUsersByRoleIds(@Param("roleIds") Collection<Long> roleIds);

    @Query("""
            select distinct u
            from User u
            join fetch u.roles r
            where upper(trim(r.name)) in :roleNames
            """)
    List<User> findDistinctUsersByRoleNames(@Param("roleNames") Collection<String> roleNames);

    @Query("""
            select distinct u
            from User u
            left join fetch u.roles
            where u.id in :userIds
            """)
    List<User> findAllWithRolesByIdIn(@Param("userIds") Collection<Long> userIds);
}
