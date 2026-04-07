package com.maharecruitment.gov.in.workorder.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.maharecruitment.gov.in.workorder.entity.WorkOrderEmployeeMappingEntity;
import com.maharecruitment.gov.in.workorder.entity.WorkOrderStatus;

public interface WorkOrderEmployeeMappingRepository extends JpaRepository<WorkOrderEmployeeMappingEntity, Long> {

    @EntityGraph(attributePaths = "workOrder")
    List<WorkOrderEmployeeMappingEntity> findByEmployeeIdOrderByWorkOrderWorkOrderDateDescWorkOrderWorkOrderIdDesc(
            Long employeeId);

    List<WorkOrderEmployeeMappingEntity> findByWorkOrderWorkOrderIdOrderByEmployeeNameAscEmployeeCodeAsc(Long workOrderId);

    @Query("select distinct mapping.employeeId "
            + "from WorkOrderEmployeeMappingEntity mapping "
            + "join mapping.workOrder workOrder "
            + "where mapping.employeeId in :employeeIds "
            + "and workOrder.active = true "
            + "and workOrder.status <> :cancelledStatus "
            + "and workOrder.effectiveFrom <= :effectiveTo "
            + "and workOrder.effectiveTo >= :effectiveFrom")
    List<Long> findEmployeeIdsWithOverlappingWorkOrders(
            @Param("employeeIds") Collection<Long> employeeIds,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo,
            @Param("cancelledStatus") WorkOrderStatus cancelledStatus);

    @Query("select distinct mapping.employeeId "
            + "from WorkOrderEmployeeMappingEntity mapping "
            + "join mapping.workOrder workOrder "
            + "where mapping.employeeId in :employeeIds "
            + "and workOrder.active = true "
            + "and workOrder.status <> :cancelledStatus")
    List<Long> findEmployeeIdsWithActiveWorkOrders(
            @Param("employeeIds") Collection<Long> employeeIds,
            @Param("cancelledStatus") WorkOrderStatus cancelledStatus);
}
