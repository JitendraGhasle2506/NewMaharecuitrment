package com.maharecruitment.gov.in.workorder.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.workorder.dto.WorkOrderForm;
import com.maharecruitment.gov.in.workorder.service.model.EmployeeWorkOrderSummaryView;
import com.maharecruitment.gov.in.workorder.service.model.WorkOrderDetailView;
import com.maharecruitment.gov.in.workorder.service.model.WorkOrderFormView;
import com.maharecruitment.gov.in.workorder.service.model.WorkOrderSummaryView;

public interface HrWorkOrderService {

    Page<WorkOrderSummaryView> getWorkOrders(Pageable pageable);

    WorkOrderFormView prepareCreateForm(Long preselectedEmployeeId, Long agencyId, String recruitmentType);

    WorkOrderFormView prepareExtensionForm(Long parentWorkOrderId);

    WorkOrderDetailView previewWorkOrder(WorkOrderForm form);

    WorkOrderDetailView generateWorkOrder(WorkOrderForm form, String actorEmail);

    /**
     * Previews one work order per valid group. A single employee type yields one preview; the "ALL" type
     * is split by employee type, department and request.
     */
    List<WorkOrderDetailView> previewWorkOrders(WorkOrderForm form);

    /** Generates one work order per valid group in a single transaction (all or nothing). */
    List<WorkOrderDetailView> generateWorkOrders(WorkOrderForm form, String actorEmail);

    WorkOrderDetailView getWorkOrderDetail(Long workOrderId);

    List<EmployeeWorkOrderSummaryView> getEmployeeWorkOrders(Long employeeId);

    String getWorkOrderDocumentPath(Long workOrderId);
}
