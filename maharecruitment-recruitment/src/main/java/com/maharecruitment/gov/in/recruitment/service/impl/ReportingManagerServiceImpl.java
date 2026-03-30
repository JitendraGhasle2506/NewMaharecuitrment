package com.maharecruitment.gov.in.recruitment.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.service.ReportingManagerService;

@Service
public class ReportingManagerServiceImpl implements ReportingManagerService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ProjectMstRepository projectRepository;

    @Autowired
    private EmployeeReportingMappingRepository mappingRepository;

    @Override
    public List<Map<String, Object>> getHodUsers() {
        List<Long> hodUserIds = new ArrayList<>();
        hodUserIds.addAll(userRepository.findDistinctUserIdsByRoleName("ROLE_HOD"));
        // Remove duplicates just in case
        hodUserIds = hodUserIds.stream().distinct().collect(Collectors.toList());

        List<User> hodUsers = userRepository.findAllById(hodUserIds);
        
        return hodUsers.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("name", u.getName() + " (" + u.getEmail() + ")");
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getManagersByType(String type) {
        String designationName = "STM".equalsIgnoreCase(type) ? "Senior Technical Manager (STM)" : "Project Manager";
        List<EmployeeEntity> managers = employeeRepository.findByDesignation_DesignationNameIgnoreCaseAndStatusIgnoreCase(designationName, "ACTIVE");
        
        return managers.stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", e.getEmployeeId());
                    map.put("name", e.getFullName() + " (" + e.getEmployeeCode() + ")");
                    return map;
                }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getProjects() {
        List<ProjectMst> projects = projectRepository.findAll();
        return projects.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getProjectId());
            map.put("name", p.getProjectName());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getInternalEmployees(Long includeEmployeeId) {
        List<EmployeeEntity> allEmployees = employeeRepository.findAll();
        Set<Long> mappedEmpIds = mappingRepository.findAll().stream().map(EmployeeReportingMappingEntity::getEmployeeId).collect(Collectors.toSet());
        return allEmployees.stream()
                .filter(e -> "INTERNAL".equalsIgnoreCase(e.getRecruitmentType()) && "ACTIVE".equalsIgnoreCase(e.getStatus()))
                .filter(e -> !mappedEmpIds.contains(e.getEmployeeId()) || e.getEmployeeId().equals(includeEmployeeId))
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", e.getEmployeeId());
                    map.put("name", e.getFullName() + " (" + e.getEmployeeCode() + ")");
                    return map;
                }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getAllMappings() {
        List<EmployeeReportingMappingEntity> mappings = mappingRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        
        List<EmployeeEntity> employees = employeeRepository.findAll();
        Map<Long, EmployeeEntity> empMap = employees.stream().collect(Collectors.toMap(EmployeeEntity::getEmployeeId, e -> e));
        
        List<User> users = userRepository.findAll();
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        
        List<ProjectMst> projects = projectRepository.findAll();
        Map<Long, ProjectMst> projMap = projects.stream().collect(Collectors.toMap(ProjectMst::getProjectId, p -> p));

        for(EmployeeReportingMappingEntity m : mappings) {
           Map<String, Object> map = new HashMap<>();
           map.put("mappingId", m.getMappingId());
           map.put("employeeId", m.getEmployeeId());
           
           EmployeeEntity emp = empMap.get(m.getEmployeeId());
           map.put("employeeName", emp != null ? emp.getFullName() + " (" + emp.getEmployeeCode() + ")" : "");
           
           map.put("projectId", m.getProjectId());
           ProjectMst proj = projMap.get(m.getProjectId());
           map.put("projectName", proj != null ? proj.getProjectName() : "");

           map.put("managerType", m.getManagerType());
           map.put("managerEmployeeId", m.getManagerEmployeeId());
           EmployeeEntity mgr = empMap.get(m.getManagerEmployeeId());
           map.put("managerName", mgr != null ? mgr.getFullName() + " (" + mgr.getEmployeeCode() + ")" : "");

           map.put("hodUserId", m.getHodUserId());
           User hod = userMap.get(m.getHodUserId());
           map.put("hodName", hod != null ? hod.getName() : "");
           
           result.add(map);
        }
        return result;
    }

    @Override
    @Transactional
    public void saveMapping(Long hodUserId, String managerType, Long managerEmployeeId, Long projectId, List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) return;

        for (Long empId : employeeIds) {
            EmployeeReportingMappingEntity mapping = mappingRepository.findByEmployeeId(empId);
            if (mapping == null) {
                mapping = new EmployeeReportingMappingEntity();
                mapping.setEmployeeId(empId);
            }
            mapping.setHodUserId(hodUserId);
            mapping.setManagerType(managerType);
            mapping.setManagerEmployeeId(managerEmployeeId);
            mapping.setProjectId(projectId);
            
            mappingRepository.save(mapping);
        }
    }
}
