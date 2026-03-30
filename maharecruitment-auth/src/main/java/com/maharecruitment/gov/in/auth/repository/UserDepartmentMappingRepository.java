package com.maharecruitment.gov.in.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.auth.entity.UserDepartmentMappingEntity;

@Repository
public interface UserDepartmentMappingRepository extends JpaRepository<UserDepartmentMappingEntity, Long> {

    List<UserDepartmentMappingEntity> findByUser_IdOrderByUserDepartmentMappingIdAsc(Long userId);

    Optional<UserDepartmentMappingEntity> findByUser_IdAndDepartmentRegistration_DepartmentRegistrationId(
            Long userId,
            Long departmentRegistrationId);

    Optional<UserDepartmentMappingEntity> findTopByUser_IdAndActiveTrueOrderByPrimaryMappingDescUserDepartmentMappingIdAsc(
            Long userId);
}
