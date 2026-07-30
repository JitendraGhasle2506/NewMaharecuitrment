package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeFcmToken;

@Repository
public interface EmployeeFcmTokenRepository extends JpaRepository<EmployeeFcmToken, Long> {

    Optional<EmployeeFcmToken> findByEmployeeIdAndDeviceId(Long employeeId, String deviceId);

    List<EmployeeFcmToken> findByEmployeeId(Long employeeId);

    List<EmployeeFcmToken> findByDeviceId(String deviceId);
}
