package com.maharecruitment.gov.in.web.service.dashboard.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.master.repository.CellMasterRepository;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.service.dashboard.ExecutiveDashboardService;
import com.maharecruitment.gov.in.web.service.dashboard.model.ExecutiveDashboardView;

@Service
public class ExecutiveDashboardServiceImpl implements ExecutiveDashboardService {

    private static final String INTERNAL = "INTERNAL";
    private static final String MAHAIT = "MAHAIT";
    private static final String EXTERNAL = "EXTERNAL";

    private final EmployeeRepository employeeRepository;
    private final ProjectMstRepository projectRepository;
    private final CellMasterRepository cellRepository;

    public ExecutiveDashboardServiceImpl(
            EmployeeRepository employeeRepository,
            ProjectMstRepository projectRepository,
            CellMasterRepository cellRepository) {
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
        this.cellRepository = cellRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ExecutiveDashboardView getDashboard() {
        return new ExecutiveDashboardView(
                employeeRepository.count(),
                employeeRepository.countByRecruitmentType(INTERNAL),
                employeeRepository.countByRecruitmentType(MAHAIT),
                employeeRepository.countByRecruitmentType(EXTERNAL),
                projectRepository.count(),
                cellRepository.count());
    }
}
