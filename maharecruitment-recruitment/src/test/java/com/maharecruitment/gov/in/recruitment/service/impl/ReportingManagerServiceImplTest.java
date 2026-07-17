package com.maharecruitment.gov.in.recruitment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class ReportingManagerServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private ReportingManagerServiceImpl service;

    @Test
    void getManagersByTypeStmQueriesStmRole() {
        EmployeeEntity emp = new EmployeeEntity();
        emp.setEmployeeId(10L);
        emp.setFullName("John Doe");
        emp.setEmployeeCode("EMP10");

        when(employeeRepository.findActiveEmployeesByRoleName("ROLE_STM")).thenReturn(List.of(emp));

        List<Map<String, Object>> result = service.getManagersByType("STM");

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).get("id"));
        assertEquals("John Doe (EMP10)", result.get(0).get("name"));
    }

    @Test
    void getManagersByTypePmQueriesPmRole() {
        EmployeeEntity emp = new EmployeeEntity();
        emp.setEmployeeId(20L);
        emp.setFullName("Jane Smith");
        emp.setEmployeeCode("EMP20");

        when(employeeRepository.findActiveEmployeesByRoleName("ROLE_PM")).thenReturn(List.of(emp));

        List<Map<String, Object>> result = service.getManagersByType("PM");

        assertEquals(1, result.size());
        assertEquals(20L, result.get(0).get("id"));
        assertEquals("Jane Smith (EMP20)", result.get(0).get("name"));
    }
}
