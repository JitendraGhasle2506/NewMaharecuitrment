package com.maharecruitment.gov.in.attendance.controller;

import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.maharecruitment.gov.in.attendance.dto.AttendanceReportDTO;
import com.maharecruitment.gov.in.attendance.service.AttendanceRegisterService;
import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.repository.DepartmentRegistrationRepository;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;
import com.maharecruitment.gov.in.master.repository.AgencyMasterRepository;
import com.maharecruitment.gov.in.master.repository.DepartmentMstRepository;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationEntity;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;

@Controller
@RequestMapping("/hr")
public class AttendanceReportController {

    @Autowired
    private AttendanceRegisterService attendanceService;
    
    @Autowired
    private DepartmentRegistrationRepository departmentRegistrationRepository;

    @Autowired
    private AgencyMasterRepository agencyMasterRepository;

    @Autowired
    private DepartmentProjectApplicationRepository departmentProjectApplicationRepository;

    @Autowired
    private DepartmentMstRepository departmentMstRepository;

    @GetMapping("/attendanceReport")
    public String externalAttendanceReport(Model model) {

        List<DepartmentRegistrationEntity> registrationDepartments = departmentRegistrationRepository.findAll();
        List<Map<String, Object>> departmentList = new ArrayList<>();

        if (registrationDepartments != null && !registrationDepartments.isEmpty()) {
            for (DepartmentRegistrationEntity regDept : registrationDepartments) {
                DepartmentMst department = departmentMstRepository
                        .findById(regDept.getDepartmentId())
                        .orElse(null);

                if (department != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("departmentId", department.getDepartmentId());
                    map.put("departmentName", department.getDepartmentName());
                    map.put("departmentRegistrationId", regDept.getDepartmentRegistrationId());
                    departmentList.add(map);
                }
            }
        }

        model.addAttribute("departmentList", departmentList);
        model.addAttribute("registrationDepartments", departmentList);
        model.addAttribute("agencyList", agencyMasterRepository.findAll());
        model.addAttribute("projectList", new ArrayList<>());

        LocalDate today = LocalDate.now();
        model.addAttribute("currentMonth", today.getMonthValue());
        model.addAttribute("currentYear", today.getYear());
        model.addAttribute("selectedMonth", today.getMonthValue());
        model.addAttribute("selectedYear", today.getYear());
        model.addAttribute("monthNames", getMonthNames());
        model.addAttribute("selectedMonthName", getMonthNames().get(today.getMonthValue()));
        model.addAttribute("daysInMonth", today.lengthOfMonth());

        return "attendance/attendance-report-external";
    }

    @PostMapping("/attendanceReport")
    public String fetchExternalAttendanceReport(
            @RequestParam(required = false) Long regDeptId,
            @RequestParam(required = false) Long agencyId,
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(required = false) Long projectId,
            Model model) {

        List<AttendanceReportDTO> reportData = attendanceService.getExternalAttendanceReportData(regDeptId, agencyId, month,
                year, projectId);
        model.addAttribute("reportData", reportData);

        List<DepartmentRegistrationEntity> registrationDepartments = departmentRegistrationRepository.findAll();
        List<Map<String, Object>> departmentList = new ArrayList<>();
        if (registrationDepartments != null && !registrationDepartments.isEmpty()) {
            for (DepartmentRegistrationEntity regDept : registrationDepartments) {
                DepartmentMst department = departmentMstRepository.findById(regDept.getDepartmentId()).orElse(null);
                if (department != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("departmentId", department.getDepartmentId());
                    map.put("departmentName", department.getDepartmentName());
                    map.put("departmentRegistrationId", regDept.getDepartmentRegistrationId());
                    departmentList.add(map);
                }
            }
        }
        model.addAttribute("registrationDepartments", departmentList);

        if (regDeptId != null) {
            model.addAttribute("projectList",
                    departmentProjectApplicationRepository.findByDepartmentRegistrationIdOrderByDepartmentProjectApplicationIdDesc(regDeptId));
        } else {
            model.addAttribute("projectList", new ArrayList<>());
        }
        
        model.addAttribute("agencyList", agencyMasterRepository.findAll());

        model.addAttribute("monthNames", getMonthNames());
        model.addAttribute("selectedRegDept", regDeptId);
        model.addAttribute("selectedAgency", agencyId);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedProject", projectId);
        model.addAttribute("selectedMonthName", getMonthNames().get(month));
        model.addAttribute("daysInMonth", LocalDate.of(year, month, 1).lengthOfMonth());

        return "attendance/attendance-report-external";
    }

    private Map<Integer, String> getMonthNames() {
        Map<Integer, String> monthMap = new TreeMap<>();
        String[] months = new DateFormatSymbols().getMonths();
        for (int i = 0; i < 12; i++) {
            monthMap.put(i + 1, months[i]);
        }
        return monthMap;
    }

    @GetMapping("/getProjects")
    @ResponseBody
    public List<Map<String, Object>> getProjects(@RequestParam Long deptRegId) {
        List<DepartmentProjectApplicationEntity> projects = departmentProjectApplicationRepository
                .findByDepartmentRegistrationIdOrderByDepartmentProjectApplicationIdDesc(deptRegId);
        return projects.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("departmentProjectApplicationId", p.getDepartmentProjectApplicationId());
            map.put("projectName", p.getProjectName());
            return map;
        }).collect(Collectors.toList());
    }
}
