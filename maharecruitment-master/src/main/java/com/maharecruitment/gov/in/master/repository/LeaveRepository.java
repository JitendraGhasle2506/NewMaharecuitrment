package com.maharecruitment.gov.in.master.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.master.entity.LeaveEntity;

@Repository
public interface LeaveRepository extends JpaRepository<LeaveEntity, Long> {

}
