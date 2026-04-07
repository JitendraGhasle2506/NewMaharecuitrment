package com.maharecruitment.gov.in.workorder.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.maharecruitment.gov.in.workorder.entity.WorkOrderEntity;

public interface WorkOrderRepository extends JpaRepository<WorkOrderEntity, Long> {

    Page<WorkOrderEntity> findAllByActiveTrue(Pageable pageable);

    Optional<WorkOrderEntity> findByWorkOrderNumberIgnoreCase(String workOrderNumber);

    @EntityGraph(attributePaths = "employeeMappings")
    @Query("select workOrder from WorkOrderEntity workOrder where workOrder.workOrderId = :workOrderId")
    Optional<WorkOrderEntity> findDetailedByWorkOrderId(@Param("workOrderId") Long workOrderId);

    List<WorkOrderEntity> findByRootWorkOrderIdOrderByVersionNumberAscWorkOrderIdAsc(Long rootWorkOrderId);
}
