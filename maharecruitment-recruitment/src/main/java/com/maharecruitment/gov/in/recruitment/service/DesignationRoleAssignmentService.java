package com.maharecruitment.gov.in.recruitment.service;

import java.util.List;

import com.maharecruitment.gov.in.recruitment.service.model.DesignationRoleAssignmentResult;
import com.maharecruitment.gov.in.recruitment.service.model.DesignationRoleAssignmentView;

public interface DesignationRoleAssignmentService {

    List<String> getAssignableRoleNames();

    List<DesignationRoleAssignmentView> getAssignments(String searchTerm);

    DesignationRoleAssignmentResult configureAndAssign(Long designationId, String roleName);

    DesignationRoleAssignmentResult assignAllConfiguredRoles();
}
